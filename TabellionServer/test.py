import shutil
import os

path = "./submitted_files";
going_to_delete = os.listdir(path)

for folder in going_to_delete:
  # print(folder)
  if os.path.isdir(os.path.join(path, folder)):
    shutil.rmtree(os.path.join(path, folder))
