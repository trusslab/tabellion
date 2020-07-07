#!/usr/bin/python

import sys
from PIL import Image

def to_matrix(l, n):
	return [l[i:i+n] for i in xrange(0, len(l), n)]

def blk_count(ll):
	c = 0
	for i in range(len(ll)):
		l = ll[i]
		if l[0]<20 and l[1]<20 and l[2]<20:
			c+=1
	#print("count=%d" % c)
	return(c)

def height(c):
	count = 0
	line_start = 0
	text_height=[]
	for i in range(len(c)):
		if c[i]>10:
			if line_start==0:
				line_start = 1
			count += 1
		else:
			if line_start==1:
				line_start=0
				text_height.append(count)
				count = 0
	print("Text height : %d\n" % text_height[0])

def line_spacing(c):
	count = 0
	line_start = 0
	space_count=[]

	for i in range(len(c)):
		if c[i]==0:
			count += 1
			line_start = 0
		else:
			if(line_start==0):
				space_count.append(count)
				count = 0
				line_start = 1
	print("Line space min : %d\n" % min(space_count))
	print("Line space max : %d\n" % max(space_count))

def line_finder(c, im):
	w, h = im.size
	for i in range(len(c)):
		if c[i]>1000 and i>(h/2):
			print("c=%d" % c[i])
			print("l_index=%d"% i)
			return (i)


def column(matrix, i):
    return [row[i] for row in matrix]

#Black pixels per row of pixels
def text_find(im):
	pixs = list(im.getdata())
	w, h = im.size
	print("width : %d" % w)
	print("height : %d\n" % h)

	pix_val = to_matrix(pixs, w)
	c = []
	for j in range(h):
		t = blk_count(pix_val[j])
		c.append(t)
	return(c)

def top_margin(im):
	pixs = list(im.getdata())
	w, h = im.size

	pix_val = to_matrix(pixs, w)
	for j in range(h):
		if( blk_count(pix_val[j])>1 ):
			print("Top margin: %d px\n" % j)
			return(j)

def left_margin(im):
	pixs = list(im.getdata())
	w, h = im.size

	pix_val = to_matrix(pixs, w)
	for j in range(h):
		if( blk_count(column(pix_val, j))>1 ):
			print("Left margin: %d px\n" % j)
			return(j)
def right_margin(im):
	pixs = list(im.getdata())
	w, h = im.size

	pix_val = to_matrix(pixs, w)
	for j in range(h):
		if( blk_count(column(pix_val, w-j-1))>1 ):
			print("Right margin: %d px\n" % j)
			return(j)
def bottom_margin(im):
	pixs = list(im.getdata())
	w, h = im.size

	pix_val = to_matrix(pixs, w)
	for j in range(h):
		if( blk_count(pix_val[h-j-1])>1 ):
			print("Bottom margin: %d px\n" % j)
			return(j)
def add_note(im1, im2):
	note_size = 134
	im3 = Image.new("L", (int(sys.argv[3]), int(sys.argv[4])))
	im3.paste(im1, (0, 0))	
	im3.paste(im2, (0, int(sys.argv[4]) - note_size))
	line = 0#[0]*int(sys.argv[3])
	#print(line)
	im3.paste(line, (0, int(sys.argv[4]) - note_size -2, int(sys.argv[3]), int(sys.argv[4]) - note_size + 1))
	#print(im3.getdata())
	return(im3)

def transform_image(im):
	# First add note to normal mode
	im_note = Image.open("note.png")
	imn = add_note(im, im_note)
	if "last" in sys.argv[1]:
		imn.save("Nlast-" + sys.argv[2] + ".png")
	else:
		imn.save("Ndoc-" + sys.argv[2] + ".png")
	'''
	# Second add review_note to review mode
	im_review_note = Image.open("review_note.png")
	imn_review = add_note(im, im_review_note)
	if "last" in sys.argv[1]:
		imn_review.save("Nlast-review" + sys.argv[2] + ".png")
	else:
		imn_review.save("Ndoc-review" + sys.argv[2] + ".png")
	'''
	
def main(argv):
	print 'Analyzing File: ', str(sys.argv[1])
	#fname = raw_input("File name?\n")
	im = Image.open(sys.argv[1])
	transform_image(im)

	c = text_find(im)
	l_index = line_finder(c, im)
	#height(c)
	line_spacing(c)
	
	top_margin(im)
	left_margin(im)
	right_margin(im)
	bottom_margin(im)

if __name__ == "__main__":
   main(sys.argv[1:])

