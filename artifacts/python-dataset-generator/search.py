import requests
import json

pos = 0
with open('cursor.log', 'r') as cur:
	pos = int(cur.read())
print "Starting at: ", pos

pos = 0

with open('unigram_data.txt', 'r') as f:
	line_number = 0
	for unigram in f:
		line_number += 1
		if line_number < pos: continue
		
		term = unigram.lower().rstrip();
		term = term.replace(' ', '+')
		limit = 50

		r = requests.get("https://en.wikipedia.org/w/api.php?action=query&list=search&srwhat=text&srprop=size|wordcount|timestamp|snippet&srlimit=" + str(limit) + "&format=json&srsearch=" + term + "&utf8=");
		
		data = r.json()
		data['queryText'] = term
		
		fileName = term.replace(' ', '_')
		with open('data/'+fileName, 'w') as out:
			json.dump(data, out);

		with open('cursor.log', 'w') as out:
			out.write(str(line_number))

		print (term)
