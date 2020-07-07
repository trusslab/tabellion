import mistune

with open('doc.md', 'r') as myfile:
    data = myfile.read()

print(mistune.markdown(data))

