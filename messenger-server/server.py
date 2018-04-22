# -*- coding: UTF-8 -*-
import fbchat
import os,time,thread,sys,argparse,re,json,fbchat
from flask import Flask
from flask import jsonify
# #Easter egg
app = Flask(__name__)
parser = argparse.ArgumentParser(description='Messenger-Server')

parser.add_argument('--user', dest='u')
parser.add_argument('--pass', dest='p')

args = parser.parse_args()
u = args.u
p = args.p
if p==None or u==None:
        sys.exit("You need to specify password and username")


messagesPending = []
#global messagesPending

# Subclass fbchat.Client and override required
# methods
class ListenBot(fbchat.Client):
	def onMessage(self, author_id, message_object, thread_id, thread_type, **kwargs):
		self.markAsDelivered(thread_id, message_object.uid)
		#fbchat.log.info("{} from {} in {}".format(message_object, thread_id, thread_type.name))
		# If you're not the author, echo
		if author_id != self.uid:
			if thread_type == fbchat.ThreadType.GROUP:
				grp = self.fetchThreadInfo(thread_id)[thread_id].name
				nme = self.fetchUserInfo(message_object.author)[message_object.author].name
				if message_object.text == None:
					text = "Empty (Probably a image)"
				else:
					text = message_object.text
				formatm = "["+grp+"] <"+nme+"> "+text
				t = {}
				t['type'] = "GROUP"
				t['trdId'] = thread_id
				t['trdName'] = grp
				t['authorId'] = message_object.author
				t['authorName'] = nme
				t['formatedMsg'] = formatm
				#global messagesPending
				messagesPending.append(t)
				newMessage(formatm)
			if thread_type == fbchat.ThreadType.USER:
				grp = self.fetchThreadInfo(thread_id)[thread_id].name
				nme = self.fetchUserInfo(message_object.author)[message_object.author].name
				if message_object.text == None:
					text = "Empty (Probably a image)"
				else:
					text = message_object.text
				formatm = "<"+nme+"> "+text
				t = {}
				t['type'] = "USER"
				t['trdId'] = thread_id
				t['trdName'] = grp
				t['authorId'] = message_object.author
				t['authorName'] = nme
				t['formatedMsg'] = formatm
				#global messagesPending
				messagesPending.append(t)
				newMessage(formatm)

def newMessage(str):
	print(str.encode('utf-8'))

@app.route('/')
def index():
	return "<html><body><center><h1>Nothing to do here</h1></center></body></html>"

@app.route('/fb/messenger/1.0/fetchUsers/<name>', methods=['GET'])
def get_Users(name):
	r = []
	t = client.fetchAllUsers()
	for i in t:
		if i.uid != u'0':
				if re.search(name, i.name, re.IGNORECASE):
					o = {}
					o['name'] = i.name
					o['uid']  = i.uid
					o['type']  = "USER"
					r.append(o)
	return jsonify(r)

@app.route('/fb/messenger/1.0/fetchGroup/<name>', methods=['GET'])
def get_Group(name):
	r = []
	t = client.searchForGroups(name)
	for i in t:
		o = {}
		o['name'] = i.name
		o['uid']  = i.uid
		o['type']  = "GROUP"
		r.append(o)
	return jsonify(r)

@app.route('/fb/messenger/1.0/fetchThread/<name>', methods=['GET'])
def get_Thread(name):
	r = []
	t = client.searchForGroups(name)
	for i in t:
		o = {}
		o['name'] = i.name
		o['uid']  = i.uid
		o['type']  = "GROUP"
		r.append(o)
		
	t = client.fetchAllUsers()
	for i in t:
		if i.uid != u'0':
			if re.search(name, i.name, re.IGNORECASE):
				o = {}
				o['name'] = i.name
				o['uid']  = i.uid
				o['type']  = "USER"
				r.append(o)
	return jsonify(r)
	
@app.route('/fb/messenger/1.0/fetchLastConv/', methods=['GET'])
def get_LastConv():
	r = []
	t = client.fetchThreadList()
	for i in t:
		o = {}
		o['name'] = i.name
		o['uid']  = i.uid
		o['type'] = client.fetchThreadInfo(i.uid)[i.uid].type.name
		r.append(o)
	return jsonify(r)

@app.route('/fb/messenger/1.0/setThread/<id>/<type>', methods=['GET'])
def get_setThread(id,type):
	if type == "GROUP":
		t = fbchat.ThreadType.GROUP
		client.setDefaultThread(id,t)
		#global currentId
		#currentId = id
	if type == "USER":
		t = fbchat.ThreadType.GROUP
		client.setDefaultThread(id,t)
		#global currentId
		#currentId = id
	return jsonify({'status':'ok'})

@app.route('/fb/messenger/1.0/send/<id>/<type>/<txt>', methods=['GET'])
def get_send(id,type,txt):
	if type == "GROUP":
                t = fbchat.ThreadType.GROUP
	if type == "USER":
                t = fbchat.ThreadType.USER
	if t:
		client.send(fbchat.Message(text=txt), thread_id=id, thread_type=t)
	return jsonify({'status':'ok'})

@app.route('/fb/messenger/1.0/send/<id>/<type>', methods=['GET'])
def get_send_without_message(id,type,txt):
	return jsonify({'status':'nop'})

@app.route('/fb/messenger/1.0/fetchUnread/', methods=['GET'])
def get_unreadMsg():
	global messagesPending
	t = messagesPending
	messagesPending = []
	return jsonify(t)

@app.route('/help/', methods=['GET'])
def get_help():
	t = []
	t.append("/fb/messenger/1.0/fetchUsers/<name>")
	t.append("/fb/messenger/1.0/fetchGroup/<name>")
	t.append("/fb/messenger/1.0/fetchThread/<name>")
	t.append("/fb/messenger/1.0/fetchLastConv/")
	t.append("/fb/messenger/1.0/setThread/<id>/<type>")
	t.append("/fb/messenger/1.0/send/<id>/<type>/<txt>")
	t.append("/fb/messenger/1.0/fetchUnread/")
	return jsonify(t)

def flaskThread():
	app.run(debug=False)

#def fbThread():


if __name__ == "__main__":
	thread.start_new_thread(flaskThread,())
	client = ListenBot(u, p,logging_level=30,user_agent="Mozilla/5.0 (Windows NT 6.3; WOW64; ; NCT50_AAP285C84A1328) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.90 Safari/537.36" )
	print("Starting listener")
	client.listen()
#       thread.start_new_thread(fbThread,())




