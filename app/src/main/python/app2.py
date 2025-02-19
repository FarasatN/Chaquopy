import os
import time
import schedule
import logging
import json
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload
# import subprocess
import tracemalloc
tracemalloc.start()


def get_network_type():
    """Check if the Android device is using Wi-Fi or Mobile Data"""
    # wifi_status = os.popen("getprop | grep 'wifi.interface'").read() #resource leak
    # mobile_data_status = os.popen("getprop | grep 'gsm.network.type'").read() #resource leak
    # proc = subprocess.Popen(["your", "command"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    # stdout, stderr = proc.communicate() # This waits for the process to complete
    # if "wlan" in wifi_status:
    #     print("wifi")
    #     return "Wi-Fi"
    # elif "LTE" in mobile_data_status or "HSPA" in mobile_data_status:
    #     print("mobile")
    #     return "Mobile Data"
    # else:
    #     print("unknown")
    #     return "Unknown"
    return "getting network state from native android sdk, not python!"

network_type = get_network_type()
print(f"Connected via: {network_type}")

def load_config(config_path='/storage/emulated/0/Download/automate/config.json'):
    """
    Loads configuration settings from a JSON file.
    The default path is 'config.json', but you can change it as needed.
    """
    if not os.path.exists(config_path):
        raise FileNotFoundError(f"Configuration file not found: {config_path}")
    with open(config_path, 'r', encoding='utf-8') as f:
        return json.load(f)

# Load configuration
config = load_config()

# Extract configuration constants
SCOPES = config.get('SCOPES')
SERVICE_ACCOUNT_FILE = config.get('SERVICE_ACCOUNT_FILE')
PARENT_FOLDER_ID = config.get('PARENT_FOLDER_ID')
LOCAL_FOLDER = config.get('LOCAL_FOLDER')
LOG_LOCATION = config.get('LOG_LOCATION')

# # Configuration constants
# SCOPES = ['https://www.googleapis.com/auth/drive']
# SERVICE_ACCOUNT_FILE = '/storage/emulated/0/Documents/automate/service_account.json'
# PARENT_FOLDER_ID = "1kEFv-4Hz2zu4RW2dTUwevvr4ejvxspgW"
# LOCAL_FOLDER = "/storage/emulated/0/Ringtones"
# LOG_LOCATION = '/storage/emulated/0/Documents/automate/upload_log.txt'

# Configuration constants
# SCOPES = ['https://www.googleapis.com/auth/drive']
# SERVICE_ACCOUNT_FILE = 'service_account.json'
# #https://drive.google.com/drive/folders/1kEFv-4Hz2zu4RW2dTUwevvr4ejvxspgW
# #PARENT_FOLDER_ID = "12F_UglF0adD2SLhR4OVGutK1w3Cq0FlA"
# PARENT_FOLDER_ID = "1kEFv-4Hz2zu4RW2dTUwevvr4ejvxspgW"
# #LOCAL_FOLDER = "/storage/emulated/0/RedmiNote9S/automate/Personal"
# LOCAL_FOLDER = "/storage/emulated/0/Ringtones"
# LOG_LOCATION = '/storage/emulated/0/Documents/upload_log.txt'


class SafeConsoleHandler(logging.StreamHandler):
    """Custom handler to safely handle console encoding"""
    def emit(self, record):
        try:
            msg = self.format(record)
            stream = self.stream
            stream.write(msg + self.terminator)
            self.flush()
        except UnicodeEncodeError:
            clean_msg = msg.encode('ascii', 'ignore').decode('ascii')
            stream.write(clean_msg + self.terminator)
            self.flush()
        except Exception:
            self.handleError(record)

def is_log_file_exist():
    if not os.path.exists(LOG_LOCATION):
        # File does not exist, create it
        try:
            with open(LOG_LOCATION, 'w', encoding='utf-8') as f:
                # You can optionally write some initial content to the file here
                f.write("Log file created.\n") # Example initial content
            print(f"File '{LOG_LOCATION}' created successfully.")
        except IOError as e:
            print(f"Error creating file '{LOG_LOCATION}': {e}")
    else:
        print(f"File '{LOG_LOCATION}' already exists.")

def configure_logging():
    is_log_file_exist()
    """Set up logging with proper encoding handling"""
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)

    # Define formatters FIRST, before handlers that use them
    file_formatter = logging.Formatter(
        '%(asctime)s - %(levelname)s - %(message)s'
    )
    console_formatter = logging.Formatter(
        '[%(levelname)s] %(message)s'
    )

    # File handler with UTF-8 encoding
    file_handler = logging.FileHandler(
        LOG_LOCATION,
        mode='a',
        encoding='utf-8'
    )
    file_handler.setFormatter(file_formatter) # Assign file_formatter

    # Safe console handler
    console_handler = SafeConsoleHandler()
    console_handler.setFormatter(console_formatter) # Assign console_formatter

    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

def rotate_log():
    """Rotate log file to maintain only last 1000 lines"""
    logger = logging.getLogger()
    file_handler = None

    # Define file_formatter here as well, to ensure it's accessible in this scope
    file_formatter = logging.Formatter(
        '%(asctime)s - %(levelname)s - %(message)s'
    )

    # Find the file handler
    for handler in logger.handlers:
        if isinstance(handler, logging.FileHandler):
            file_handler = handler
            break

    if not file_handler:
        logging.error("No file handler found for log rotation")
        return

    log_file = file_handler.baseFilename

    try:
        # Close and remove current file handler
        file_handler.close()
        logger.removeHandler(file_handler)

        # Read existing log content
        if os.path.exists(log_file):
            with open(log_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            # Keep only last 1000 lines
            if len(lines) > 1000:
                keep_lines = lines[-1000:]
                with open(log_file, 'w', encoding='utf-8') as f:
                    f.writelines(keep_lines)

        # Create new file handler
        new_handler = logging.FileHandler(log_file, mode='a', encoding='utf-8')
        new_handler.setFormatter(file_formatter) # Assign file_formatter
        logger.addHandler(new_handler)

    except Exception as e:
        logging.error("Log rotation failed: %s", str(e))
    finally:
        # Ensure we always have a file handler
        if not any(isinstance(h, logging.FileHandler) for h in logger.handlers):
            new_handler = logging.FileHandler(log_file, mode='a', encoding='utf-8')
            new_handler.setFormatter(file_formatter) # Assign file_formatter
            logger.addHandler(new_handler)

def authenticate():
    """Authenticate using service account credentials"""
    try:
        creds = service_account.Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE, scopes=SCOPES)
        logging.info("Authentication successful")
        return creds
    except Exception as e:
        logging.error("Authentication failed: %s", str(e))
        raise

def upload_file(file_path, service, max_retries=5):
    """Upload file to Google Drive with retry logic"""
    file_name = os.path.basename(file_path)
    logging.info("Starting upload process for: %s", file_name)

    file_metadata = {'name': file_name, 'parents': [PARENT_FOLDER_ID]}
    media = MediaFileUpload(file_path, resumable=True)

    for attempt in range(1, max_retries + 1):
        try:
            logging.debug("Attempt %d/%d - %s", attempt, max_retries, file_name)
            request = service.files().create(
                body=file_metadata,
                media_body=media,
                fields='id'
            )
            file = request.execute()
            if file_id := file.get('id'):
                logging.info("Upload successful: %s (ID: %s)", file_name, file_id)
                return file_id
            raise Exception("No file ID returned from API")

        except Exception as e:
            if attempt < max_retries:
                wait_time = 2 ** attempt
                logging.warning("Attempt %d failed for %s: %s. Retrying in %ds...",
                                attempt, file_name, str(e), wait_time)
                time.sleep(wait_time)
            else:
                logging.error("Final attempt failed for %s: %s", file_name, str(e))

    return None

def process_files():
    """Process files with detailed tracking"""
    logging.info("Starting new file processing job")
    start_time = time.time()
    stats = {'processed': 0, 'success': 0, 'failures': 0}

    try:
        # Check for files first
        all_files = []
        for root, _, files in os.walk(LOCAL_FOLDER):
            all_files.extend([os.path.join(root, f) for f in files])

        if not all_files:
            logging.info("No files found in %s, skipping processing", LOCAL_FOLDER)
            return

        logging.info("Found %d files in %s", len(all_files), LOCAL_FOLDER)

        creds = authenticate()
        service = build('drive', 'v3', credentials=creds)

        for idx, file_path in enumerate(all_files, 1):
            stats['processed'] += 1
            logging.info("Processing file %d/%d: %s",
                         idx, len(all_files), os.path.basename(file_path))

            if file_id := upload_file(file_path, service):
                try:
                    os.remove(file_path)
                    stats['success'] += 1
                    logging.info("Successfully deleted %s", os.path.basename(file_path))
                except Exception as e:
                    stats['failures'] += 1
                    logging.error("Deletion failed for %s: %s",
                                  os.path.basename(file_path), str(e))
            else:
                stats['failures'] += 1
                logging.error("Upload failed for %s", os.path.basename(file_path))

    except Exception as e:
        logging.error("Critical error: %s", str(e))
    finally:
        duration = time.time() - start_time
        logging.info(
            "Job completed in %.2f seconds\n"
            "Processed: %d | Success: %d | Failures: %d",
            duration, stats['processed'], stats['success'], stats['failures']
        )
        rotate_log()

def main():
    """Main scheduler with safe logging"""
    if get_network_type()!=None:
        configure_logging()

        job_interval = 10  # seconds
        schedule.every(job_interval).seconds.do(process_files)
        logging.info("Scheduled job configured to run every %d seconds", job_interval)

        logging.info("Starting scheduler")
        while True:
            try:
                if get_network_type()!=None:
                    is_log_file_exist()
                    pending = schedule.get_jobs()
                    logging.debug("Pending jobs check: %d jobs in queue", len(pending))
                    schedule.run_pending()
            except Exception as e:
                logging.error("Scheduler error: %s", str(e))

            sleep_time = 5
            is_log_file_exist()
            logging.debug("Sleeping for %d seconds", sleep_time)
            time.sleep(sleep_time)

if __name__ == '__main__':
    main()