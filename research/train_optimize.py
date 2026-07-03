import pandas as pd
import glob
import os

data_directory = os.getenv("DATASET_PATH")
all_files = glob.glob(os.path.join(data_directory, '*_keystrokes.txt'))

eval_files = all_files[-50:]

print(f"Extracting data from {len(eval_files)} users...")

master_df_list = []

for user_idx, file_path in enumerate(eval_files):
    try:
        df = pd.read_csv(file_path, sep='\t')
        df = df.sort_values(by=['TEST_SECTION_ID', 'PRESS_TIME'])

        df['DWELL_TIME'] = (df['RELEASE_TIME'] - df['PRESS_TIME']) / 1000.0
        df['FLIGHT_TIME'] = df.groupby('TEST_SECTION_ID')['PRESS_TIME'].diff() / 1000.0

        df['FLIGHT_TIME'] = df['FLIGHT_TIME'].fillna(0)
        df['FLIGHT_TIME'] = df['FLIGHT_TIME'].clip(upper=1.0)
        df['DWELL_TIME'] = df['DWELL_TIME'].clip(upper=1.0)

        df['SUBJECT_ID'] = user_idx

        df_clean = df[['SUBJECT_ID', 'DWELL_TIME', 'FLIGHT_TIME']]
        master_df_list.append(df_clean)

    except Exception as e:
        print(f"Skipped file {file_path} due to error: {e}")
        continue

print("Concatenating files...")
df_final = pd.concat(master_df_list, ignore_index=True)

print(f"Master DataFrame created with {len(df_final)} rows.")
print("Saving to aalto_cleaned_data.csv...")

df_final.to_csv("aalto_cleaned_data.csv", index=False)
print("Success! The data is ready for the Optimization Notebook.")
