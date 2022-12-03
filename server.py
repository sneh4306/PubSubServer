import flask
from flask import request,jsonify
from collections import defaultdict
import requests
import threading
import random
from flask import Flask, render_template,request,redirect,url_for 
from bson import ObjectId 
from pymongo import MongoClient 
import os

app = flask.Flask(__name__)
app.config["DEBUG"] = True

# client = MongoClient("mongodb://127.0.0.1:27017") #host uri  
client = MongoClient("mongodb+srv://root:passwordroot@cluster0.18gmih5.mongodb.net/?retryWrites=true&w=majority")
db = client["pub_sub"] #Select the database  
publishers_collection = db["publishers"] #Select the collection name
subscribers_collection = db["subscribers"]
topics_collection = db["topics"]
masterQueue=""
queueList = {}

leaderElectionFlag = False

def findMajority(n,queues):
    temp_dict = defaultdict(int)
    majorityNumber = 0
    data = {}
    data['lower_limit'] = 0
    data['upper_limit'] = n-1
    for i in range(n):
        r = requests.post("http://"+queues[i]+"/electMaster", json=data)
        # print(r.text)
        # print(type(r))
        temp_dict[r.text]+=1
        if temp_dict[r.text]>majorityNumber:
            majorityNumber = temp_dict[r.text]
    if majorityNumber == 1:
        return findMajority(n,queues)
    return majorityNumber
            

def leaderElection():
    global leaderElectionFlag
    global masterQueue
    leaderElectionFlag=True
    del queueList[masterQueue]
    queues = list(queueList.keys())
    ind = -1

    n = len(queues)
    if n<=2:
        ind = random.randint(0,n-1)
    else:
        ind = findMajority(n,queues)
    masterQueue = queues[ind]
    leaderElectionFlag=False

    data = {}
    data["serverList"] = queues
 
    res = requests.post("http://"+masterQueue+"/updateServerList",json=data)
    while res.text == "Illegal Request":
        leaderElection()
        res = requests.post("http://"+masterQueue+"/updateServerList",json=data)
    print(masterQueue)
    print(queues)
 
    

@app.route('/getMessage', methods=['POST'])
def getMessage():
    global leaderElectionFlag
    global masterQueue
    data = request.get_json(force=True)
    topic = data['topic']
    publisher = data['publisher']
    message = data['message']
 
    query_results = list(topics_collection.find({"topic_ip":topic}))
    publishers = []
    topics = []
    query_results_publisher = list(publishers_collection.find({}))
    if len(list(query_results_publisher))>0:
        for p in list(query_results_publisher):
      
            publishers.append(p["publisher_ip"])
    if len(list(query_results))>0:
        for t in list(query_results):
       
            topics.append(t["topic_ip"])
    
    if publisher not in publishers:
        return "Add Publisher"
    if topic not in topics:
        return "Add Topic"
    if len(list(query_results))>0 and publisher not in list(query_results)[0]["publisher_list"]:
        return "Topic not added as part of publisher"
    if leaderElectionFlag:
        return "Please try again later"
    else:
        try:
            res = requests.post("http://"+masterQueue+"/addMessage",json = data)
            while res.text == "Illegal Request":
                leaderElection()
                res = requests.post("http://"+masterQueue+"/addMessage",json = data)

                return "Please try again later"
            else:
                return "Successful"

        except requests.exceptions.ConnectionError:
            if not leaderElectionFlag:
   
                thr = threading.Thread(target=leaderElection, args=(), kwargs={})
                thr.start()
            return "Please try again later"
        


        
@app.route('/addTopic', methods=['POST'])
def addTopic():
    data = request.get_json(force=True)
    topic = data['topic']
    publisher = data['publisher']
    query_results = list(topics_collection.find({"topic_ip":topic}))
    publishers = []
    query_results_publisher = list(publishers_collection.find({}))
    if len(list(query_results_publisher))>0:
        for p in list(query_results_publisher):
         
            publishers.append(p["publisher_ip"])
    if publisher not in publishers:
        return "please register as publisher"
    elif len(list(query_results))>0 and publisher in list(query_results)[0]["publisher_list"]:
        return "topic already existing in publisher"
    else:
  
        if len(list(query_results)) == 0:
            topics_collection.insert_one({"topic_ip":topic,"publisher_list":[publisher]})
        else:
            k = list(query_results[0]["publisher_list"])
            print(k)
            k.append(publisher)
            topics_collection.update_one({"topic_ip":topic},{"$set":{"publisher_list":k}})
        return "successfully added topic to publisher"

    
@app.route('/registerQueue', methods=['POST'])
def registerQueue():
    global masterQueue
    data = request.get_json(force=True)
    queue = data['queue']
    if queue in queueList:
        return "queue already existing"
    else:
        queueList[queue] = 1
        if len(masterQueue) == 0:
            masterQueue = queue
            data1 = {}
            data1["serverList"] = list(queueList.keys())
            res = requests.post("http://"+masterQueue+"/updateServerList",json=data1)
      
        else:
            res = requests.post("http://"+masterQueue+"/addQueue",json=data)
            while res.text == "Illegal Request":
                leaderElection()
                res = requests.post("http://"+masterQueue+"/addQueue",json=data)
        return "Successfully registered queue"
    
@app.route('/addPublisher', methods=['POST'])
def addPublisher():
    data = request.get_json(force=True)
    publisher = data['publisher']
    
    publishers = []
    query_results_publisher = list(publishers_collection.find({}))
    if len(list(query_results_publisher))>0:
        for p in list(query_results_publisher):
        
            publishers.append(p["publisher_ip"])
        
    if publisher in publishers:
       
        return "publisher already registered"
    else:
   
        publishers_collection.insert_one({"publisher_ip":publisher})
        return "publisher registered successfully"
    
@app.route('/serverDown', methods=['POST'])
def serverDown():
    data = request.get_json(force=True)
    queue = data["queue"]
    del queueList[queue]
    return "successful"
    
        
        
if __name__ == '__main__':
    app.run(host="localhost", port=5000, debug=True)
