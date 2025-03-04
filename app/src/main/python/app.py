import os
import time
import json
import logging
import requests  # For network speed measurement
import tracemalloc
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

tracemalloc.start()
#
# --------------------------
# Configuration Management
# --------------------------
class ConfigManager:
    _instance = None
    _config = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ConfigManager, cls).__new__(cls)
            cls._instance.load_config()
        return cls._instance

    def load_config(self, config_path='/storage/emulated/0/Download/automate/config.json'):
        if not os.path.exists(config_path):
            raise FileNotFoundError(f"Configuration file not found: {config_path}")
        with open(config_path, 'r', encoding='utf-8') as f:
            self._config = json.load(f)
        # Optionally, validate required keys here.
        return self._config

    def get(self, key, default=None):
        return self._config.get(key, default)

config = ConfigManager()

SCOPES = config.get('SCOPES')

SERVICE_ACCOUNT_FILE = config.get('SERVICE_ACCOUNT_FILE')

PARENT_FOLDER_ID = config.get('PARENT_FOLDER_ID')

LOCAL_FOLDER = config.get('LOCAL_FOLDER')

LOG_LOCATION = config.get('LOG_LOCATION')

KEEP_LOG_LINE_COUNT = 1000
KEEP_LOG_LINE_COUNT = config.get('KEEP_LOG_LINE_COUNT')

DEFAULT_CHUNK_SIZE_FOR_MEASURING_NETWORK_SPEED = 1024
DEFAULT_CHUNK_SIZE_FOR_MEASURING_NETWORK_SPEED = config.get('DEFAULT_CHUNK_SIZE_FOR_MEASURING_NETWORK_SPEED')

DEFAULT_TIMEOUT_FOR_MEASURING_NETWORK_SPEED = 10
DEFAULT_TIMEOUT_FOR_MEASURING_NETWORK_SPEED = config.get('DEFAULT_TIMEOUT_FOR_MEASURING_NETWORK_SPEED')

UPLOAD_PROGRESS_PERCENTAGE = 1
UPLOAD_PROGRESS_PERCENTAGE = config.get('UPLOAD_PROGRESS_PERCENTAGE')

MAX_RETRIES = 20
MAX_RETRIES = config.get('MAX_RETRIES')

SLEEP_TIME = 20
SLEEP_TIME = config.get('SLEEP_TIME')


# --------------------------
# Logging Setup
# --------------------------
class SafeConsoleHandler(logging.StreamHandler):
    def emit(self, record):
        try:
            msg = self.format(record)
            self.stream.write(msg + self.terminator)
            self.flush()
        except UnicodeEncodeError:
            clean_msg = msg.encode('ascii', 'ignore').decode('ascii')
            self.stream.write(clean_msg + self.terminator)
            self.flush()
        except Exception:
            self.handleError(record)

def is_log_file_exist():
    if not os.path.exists(LOG_LOCATION):
        try:
            os.makedirs(os.path.dirname(LOG_LOCATION), exist_ok=True)
            with open(LOG_LOCATION, 'w', encoding='utf-8') as f:
                f.write("Log file created.\n")
            print(f"File '{LOG_LOCATION}' created successfully.")
        except IOError as e:
            print(f"Error creating file '{LOG_LOCATION}': {e}")
    else:
        print(f"File '{LOG_LOCATION}' already exists.")

def configure_logging():
    is_log_file_exist()
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    file_formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    console_formatter = logging.Formatter('[%(levelname)s] %(message)s')
    file_handler = logging.FileHandler(LOG_LOCATION, mode='a', encoding='utf-8')
    file_handler.setFormatter(file_formatter)
    console_handler = SafeConsoleHandler()
    console_handler.setFormatter(console_formatter)
    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

def rotate_log():
    logger = logging.getLogger()
    file_handler = None
    file_formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
    for handler in logger.handlers:
        if isinstance(handler, logging.FileHandler):
            file_handler = handler
            break
    if not file_handler:
        logging.error("No file handler found for log rotation")
        return
    log_file = file_handler.baseFilename
    try:
        file_handler.close()
        logger.removeHandler(file_handler)
        if os.path.exists(log_file):
            with open(log_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()
            if len(lines) > KEEP_LOG_LINE_COUNT:
                keep_lines = lines[-KEEP_LOG_LINE_COUNT:]
                with open(log_file, 'w', encoding='utf-8') as f:
                    f.writelines(keep_lines)
        new_handler = logging.FileHandler(log_file, mode='a', encoding='utf-8')
        new_handler.setFormatter(file_formatter)
        logger.addHandler(new_handler)
    except Exception as e:
        logging.error("Log rotation failed: %s", str(e))
    finally:
        if not any(isinstance(h, logging.FileHandler) for h in logger.handlers):
            new_handler = logging.FileHandler(log_file, mode='a', encoding='utf-8')
            new_handler.setFormatter(file_formatter)
            logger.addHandler(new_handler)

# --------------------------
# Network Speed Measurement
# --------------------------
def measure_network_speed(test_url="https://www.google.com", timeout=DEFAULT_TIMEOUT_FOR_MEASURING_NETWORK_SPEED):
    start_time = time.time()
    total_bytes = 0
    try:
        r = requests.get(test_url, timeout=timeout, stream=True)
        # Read 100KB of data for the test
        for chunk in r.iter_content(chunk_size=DEFAULT_CHUNK_SIZE_FOR_MEASURING_NETWORK_SPEED):
            total_bytes += len(chunk)
            if total_bytes >= 100 * 1024:
                break
        elapsed = time.time() - start_time
        if elapsed > 0:
            speed_mbps = (total_bytes * 8) / (elapsed * 1e6)
            logging.debug(f"Measured network speed: {speed_mbps:.2f} Mbps")
            return speed_mbps
    except Exception as e:
        logging.warning(f"Network speed measurement failed: {e}")
        return 0.5  # Fallback to very slow speed (0.5 Mbps)
    return 0.5

# --------------------------
# Chunk Size Calculation
# --------------------------
def calculate_chunk_size(file_size):
    network_speed = measure_network_speed()  # in Mbps
    if network_speed < 0.1:
        network_speed = 0.5
    if file_size < 10 * 1024 * 1024:    # < 10MB
        return 100 * 1024 * 1024  # Return a large chunk size (100MB) instead of None - EFFECTIVELY DIRECT UPLOAD
    elif file_size < 128 * 1024 * 1024:  # < 128MB
        if network_speed < 1:
            return 1 * 1024 * 1024  # 1MB chunks for slow networks
        else:
            return 2 * 1024 * 1024  # 2MB chunks
    else:
        if network_speed < 1:
            return 2 * 1024 * 1024  # 2MB chunks
        elif network_speed < 5:
            return 5 * 1024 * 1024  # 5MB chunks
        else:
            return 10 * 1024 * 1024  # 10MB chunks for fast networks
# --------------------------
# Drive Upload Functionality
# --------------------------
def authenticate():
    try:
        creds = service_account.Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE, scopes=SCOPES)
        logging.info("Authentication successful")
        return creds
    except Exception as e:
        logging.error("Authentication failed: %s", str(e))
        raise

def update_progress(current, total, file_name):
    progress = (current / total) * 100 * UPLOAD_PROGRESS_PERCENTAGE
    # logging.info(f"Uploading {file_name}: {progress:.1f}% complete")
    # logging.info(f"Uploading {file_name}: {progress:.1f}% complete")
    progress_rounded = round(progress)  # Round to the nearest whole number
    logging.info(f"Uploading {file_name}: {progress_rounded}% complete") # Use the rounded value

def upload_file(file_path, service, max_retries=MAX_RETRIES):
    file_name = os.path.basename(file_path)
    file_size = os.path.getsize(file_path)
    chunk_size = calculate_chunk_size(file_size)
    logging.info(f"Starting upload for {file_name} ({file_size} bytes) with chunk size: {chunk_size if chunk_size else 'Direct upload'}")
    file_metadata = {'name': file_name, 'parents': [PARENT_FOLDER_ID]}
    media = MediaFileUpload(file_path, mimetype='application/octet-stream',
                            resumable=bool(chunk_size), chunksize=chunk_size)
    for attempt in range(1, max_retries + 1):
        try:
            logging.debug(f"Attempt {attempt}/{max_retries} for {file_name}")
            request = service.files().create(
                body=file_metadata,
                media_body=media,
                fields='id'
            )
            response = None
            while response is None:
                status, response = request.next_chunk() # timeout=300 # 5 minute timeout per chunk
                if status:
                    update_progress(status.resumable_progress, file_size, file_name)
            if file_id := response.get('id'):
                logging.info(f"Upload successful: {file_name} (ID: {file_id})")
                return file_id
            raise Exception("No file ID returned after chunk upload")
        except Exception as e:
            if attempt < max_retries:
                wait_time = 2 ** attempt
                logging.warning(f"Attempt {attempt} failed for {file_name}: {e}. Retrying in {wait_time}s...")
                time.sleep(wait_time)
            else:
                logging.error(f"Final attempt failed for {file_name}: {e}")
    return None

def process_files():
    logging.info("Starting file processing job")
    start_time = time.time()
    stats = {'processed': 0, 'success': 0, 'failures': 0}
    try:
        all_files = []
        for root, _, files in os.walk(LOCAL_FOLDER):
            all_files.extend([os.path.join(root, f) for f in files])
        if not all_files:
            logging.info(f"No files found in {LOCAL_FOLDER}.")
            return 0
        logging.info(f"Found {len(all_files)} files in {LOCAL_FOLDER}.")
        creds = authenticate()
        service = build('drive', 'v3', credentials=creds)
        for idx, file_path in enumerate(all_files, 1):
            stats['processed'] += 1
            logging.info(f"Processing file {idx}/{len(all_files)}: {os.path.basename(file_path)}")
            if upload_file(file_path, service):
                try:
                    os.remove(file_path)
                    stats['success'] += 1
                    logging.info(f"Deleted local file: {os.path.basename(file_path)}")
                except Exception as e:
                    stats['failures'] += 1
                    logging.error(f"Failed to delete {os.path.basename(file_path)}: {e}")
            else:
                stats['failures'] += 1
                logging.error(f"Upload failed for {os.path.basename(file_path)}")
    except Exception as e:
        logging.error(f"Critical error during file processing: {e}")
    finally:
        duration = time.time() - start_time
        logging.info(f"Job completed in {duration:.2f} seconds. Processed: {stats['processed']} | Success: {stats['success']} | Failures: {stats['failures']}")
        rotate_log()
        return stats['processed']

# --------------------------
# Main Scheduler
# --------------------------
def main():
    configure_logging()
    while True:
        try:
            files_processed = process_files()
            if files_processed == 0:
                logging.info("No files to process. Exiting ..")
                break
        except Exception as e:
            logging.error(f"Error: {e}")
        # Sleep default 20 seconds between checks
        logging.debug(f"Sleeping for {SLEEP_TIME} seconds before next check.")
        time.sleep(SLEEP_TIME)

if __name__ == '__main__':
    main()
