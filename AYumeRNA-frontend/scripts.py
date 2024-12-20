import requests
import json
import csv
from typing import List, Dict

# Base URL for the server
BASE_URL = "https://isee-trna.lumoxuan.cn"  # Replace with your actual server URL

# API endpoints
GENERATE_API = f"{BASE_URL}/sample/process"
PREDICT_API = f"{BASE_URL}/r2dt/run"
CHARGE_API = f"{BASE_URL}/charge"

# File paths for saving output
SEQUENCE_OUTPUT_FILE = "generated_sequences.csv"
PREDICTION_OUTPUT_FILE = "prediction_results.json"


def generate_sequences(model: str, reverse_codon: str, sequence_count: int) -> List[Dict]:
    """
    Call the API to generate sup-tRNA sequences.
    :param model: Model name
    :param reverse_codon: Reverse codon
    :param sequence_count: Number of sequences to generate
    :return: List of generated sequences
    """
    params = {
        "model": model,
        "reverseCodon": reverse_codon,
        "sequenceCount": sequence_count,
    }

    print("[INFO] Generating sequences...")
    response = requests.post(GENERATE_API, data=params)

    if response.status_code == 200:
        sequences = response.json().get("sequences", [])
        print(f"[INFO] Successfully generated {len(sequences)} sequences.")
        return sequences
    else:
        print("[ERROR] Failed to generate sequences:", response.text)
        return []


def save_sequences_to_csv(sequences: List[Dict], file_path: str):
    """
    Save generated sequences to a CSV file.
    :param sequences: List of sequences
    :param file_path: File path for saving
    """
    print(f"[INFO] Saving sequences to {file_path}...")
    with open(file_path, "w", newline="") as csvfile:
        writer = csv.writer(csvfile)
        writer.writerow(["Sequence", "Anticodon", "InfernalScore", "tRNAStart", "tRNAEnd", "tRNAType"])
        for seq in sequences:
            writer.writerow([seq.get("sequence"), seq.get("anticodon"), seq.get("infernalScore"),
                             seq.get("tRNAStart"), seq.get("tRNAEnd"), seq.get("tRNAType")])
    print("[INFO] Sequences saved successfully.")


def predict_secondary_structure(sequence: str) -> Dict:
    """
    Call the API to get the secondary structure prediction for a tRNA.
    :param sequence: tRNA sequence
    :return: Prediction result
    """
    print("[INFO] Predicting secondary structure...")
    response = requests.post(PREDICT_API, json={"sequence": sequence})

    if response.status_code == 200:
        prediction = response.json()
        print("[INFO] Secondary structure prediction completed.")
        return prediction
    else:
        print("[ERROR] Failed to predict secondary structure:", response.text)
        return {}


def calculate_free_energy(sequence: str) -> Dict:
    """
    Call the API to calculate free energy for a tRNA sequence.
    :param sequence: tRNA sequence
    :return: Free energy calculation result
    """
    print("[INFO] Calculating free energy for the sequence...")
    response = requests.post(CHARGE_API, json={"sequence": sequence})

    if response.status_code == 200:
        free_energy_data = response.json()
        print("[INFO] Free energy calculation completed.")
        return free_energy_data
    else:
        print("[ERROR] Failed to calculate free energy:", response.text)
        return {}


def save_prediction_to_file(prediction: Dict, file_path: str):
    """
    Save prediction results to a file.
    :param prediction: Prediction result
    :param file_path: File path for saving
    """
    print(f"[INFO] Saving prediction results to {file_path}...")
    with open(file_path, "w") as jsonfile:
        json.dump(prediction, jsonfile, indent=4)
    print("[INFO] Prediction results saved successfully.")


if __name__ == "__main__":
    # Example parameters
    selected_model = "example_model"  # Replace with the actual model name
    reverse_codon = "UAG"  # Replace with the actual reverse codon
    sequence_count = 10

    # Step 1: Generate sequences
    generated_sequences = generate_sequences(selected_model, reverse_codon, sequence_count)

    if generated_sequences:
        # Save generated sequences to a file
        save_sequences_to_csv(generated_sequences, SEQUENCE_OUTPUT_FILE)

        # Step 2: Get secondary structure prediction for each sequence
        for seq in generated_sequences:
            sequence = seq.get("sequence", "")
            prediction = predict_secondary_structure(sequence)
            save_prediction_to_file(prediction, f"prediction_{sequence[:10]}.json")  # Save each sequence's prediction result

            # Step 3: Calculate free energy
            free_energy_data = calculate_free_energy(sequence)
            print(f"Free energy for {sequence[:10]}: {free_energy_data}")

    print("[INFO] Script execution completed.")