from PIL import Image
import imagehash
import sys

hash0 = imagehash.average_hash(Image.open(sys.argv[1])) 
hash1 = imagehash.average_hash(Image.open(sys.argv[2])) 
cutoff = 5

if hash0 - hash1 < cutoff:
  print('images are similar')
else:
  print('images are not similar')