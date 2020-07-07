

#assuming 5pages
python mdtohtml.py > doc.html
cat style.txt doc.html > Ndoc.html
xvfb-run -a wkhtmltopdf Ndoc.html doc.pdf

#add note & checker
xvfb-run -a wkhtmltoimage --height 134 --width 1080 note.html note.png
pdftoppm -scale-to-x 1080 -scale-to-y 1786 doc.pdf doc -png
# ./checker.sh
