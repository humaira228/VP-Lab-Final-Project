import gdown
import os

MODELS = {
    "ml/health_model.pkl": "12A_VSZ3MfgIegXFMZduTOmyWO_JRxCMj",
    # Add other models here with their respective File IDs
}

def download_models():
    for path, file_id in MODELS.items():
        if not os.path.exists(path):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            url = f"https://drive.google.com/uc?id={file_id}"
            print(f"Downloading {path}...")
            gdown.download(url, path, quiet=False)

if __name__ == "__main__":
    download_models()
