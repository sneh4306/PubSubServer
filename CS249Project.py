from flask import Flask, request, json, Response
from collections import deque
import requests
import threading
import sys
import random

app = Flask(__name__)
app.config["DEBUG"] = True

queue = deque()
master = False
serverList = []
coordinatorServer = ""
queueLock = threading.Lock()
consumerServer = ""
myserverIP = ""

def sendUpdateMessage(server,response):
    print('http://'+server + '/update')
    res = requests.post('http://'+server + '/update', json=response)
    print(res.status_code)
    if res.status_code == 200:
        return True
    return False

def informPubSubServer(server):
    requests.post('http://'+coordinatorServer + '/serverDown', json={"queue": server})
    serverList.remove(server)

def retrySend(response, servers):
    for server in servers:
        if sendUpdateMessage(server, response) == False:
            informPubSubServer(server)

def addToAllQueue(response):
    errorWithServer = []
    print("LL")
    for server in serverList:
        print(server)
        if sendUpdateMessage(server,response) == False:
            errorWithServer.append(server)
    
    if errorWithServer != None:
        retrySend(response,errorWithServer)

@app.route('/addMessage',methods=['POST'])
def addToQueue():
    global queue
    global master
    print(master)
    print("PP")
    print(request)
    if master:
        response = request.get_json()
        print(response)
        if 'topic' in response and 'message' in response and 'publisher' in response:
            queueLock.acquire()
            queue.append(response)
            queueLock.release()
            addToAllQueue(response)
            if len(queue) == 1:
                triggerAttempt()
            return Response("Successful",status = '200')
    else:
        return Response("Illegal Request", status= '404')


@app.route("/update", methods = ['POST'])
def updateQueue():
    print(request.get_json())
    if not master:
        res = request.get_json()
        try:
            queueLock.acquire()
            queue.append(res)
            queueLock.release()
            return Response("Successful", status="200")
        except:
            return Response("Failed", status='400')
    else:
        return Response("Successful", status='200')

def registerQueueAsMaster():
    # #Only for test
    # return True
    print('http://'+consumerServer + '/subserver/queue-id?ipAddress=' + myserverIP)
    response = requests.get('http://'+consumerServer + '/subserver/queue-id?ipAddress=' + myserverIP)
    if response.status_code == 200:
        return True
    return False

# @app.route('/ping', methods="GET")
# def ping():
#     res = requests.get('http://'+consumerServer + '/subserver)

@app.route('/updateServerList',methods=['POST'])
def updateServerList():
    global serverList
    global master
    print(request)
    response = request.get_json()
    print(response['serverList'])
    if 'serverList' in response:
        if registerQueueAsMaster():
            serverList = response['serverList']
            master = True
            return Response("Successful", status = '200')
    return Response("Illegal Request", status='404')


@app.route('/remove',methods=['POST'])
def removeFromQueue():
    global queue
    global master
    print(master)
    print("PP")
    print(request)
    if master == False:
        if len(queue) > 0:
            queueLock.acquire()
            res = queue.popleft()
            queueLock.release()
            return Response(res, status='200')
        return Response(None,status='400')
    return Response("Cannot be done on Master", status= '200')

def sendRemoveMessage(server):
    res = requests.post('http://'+server + '/remove')
    print(res.status_code)
    if res.status_code == 200:
        return True
    return False

def retryRemove(servers):
    for server in servers:
        if sendRemoveMessage(server) == False:
            informPubSubServer(server)

def removeFromAllQueue():
    errorWithServer = []
    print(serverList)
    for server in serverList:
        if sendRemoveMessage(server) == False:
            errorWithServer.append(server)
    
    print(errorWithServer)
    if errorWithServer != None:
        retryRemove(errorWithServer)

@app.route('/nextMessage', methods=['GET'])
def nextMessage():
    global queue
    global queueLock
    print(queueLock)
    if master:
        if len(queue) > 0:
            queueLock.acquire()
            res = queue.popleft()
            queueLock.release()
            print(res)
            removeFromAllQueue()
            v = json.dumps(res)
            r2 = Response(v, status='200')
            r2.headers.add('Content-Type', 'application/json')
            return r2
        else:
            return Response("Queue Empty", status='404')
    return Response("Works only on Master Queue",status='404')

@app.route('/printQueue', methods=['GET'])
def printQueue():
    queueLock.acquire()
    for i in queue:
        print(i)
    queueLock.release()
    return Response("Successful", status='200')

@app.route('/replicateQueue', methods = ['POST'])
def replicateQueue():
    print(request)
    print(request.get_json())
    mainQueue = request.get_json()["queue"]
    print(mainQueue)
    global queue
    try:
        queueLock.acquire()
        queue = deque(mainQueue)
        queueLock.release()
        return Response("Successful", status = '200')
    except:
        return Response("Cannot Replicate Queue", status='400')

    
def triggerAttempt():
    #Only for Test
    # return True
    res = requests.get('http://'+consumerServer + '/subserver/trigger')
    print(res)

    

@app.route('/addQueue', methods = ['POST'])
def addNewQueue():
    global serverList
    global queue
    if master:
        server = request.get_json()["queue"]
        print(server)
        try:
            queueLock.acquire()
            dictm = {}
            dictm["queue"] = list(queue)
            print(dictm)
            print((str(server) + '/replicateQueue'))
            res = requests.post('http://'+(str(server) + '/replicateQueue'), json = dictm)
            if res.status_code == 200:
                serverList.append(server)
                print(serverList)
            queueLock.release()
            if res.status_code != 200:
                return res
            return Response("Server Added", status='200')
        except:
            return "Illegal Request"
    return "Illegal Request"


@app.route('/registerConsumerServer', methods = ['POST'])
def registerConsumerServer():
    global consumerServer
    server = request.get_json()['consumerServer']
    consumerServer = server
    return Response('Registered Consumer', status='200')

@app.route('/registerCoordinatorServer', methods = ['POST'])
def registerCoordinatorServer():
    global coordinatorServer
    server = request.get_json()['coordinatorServer']
    coordinatorServer = server
    return Response('Registered Coordinator', status='200')

@app.route('/register', methods=['POST'])
def register():
    global myserverIP
    myserverIP = request.get_json()['server']
    dictm = {}
    dictm["queue"] = myserverIP
    requests.post("http://"+coordinatorServer + '/registerQueue', json=dictm)
    return Response('Registered',status='200')

@app.route('/electMaster', methods= ['POST'])
def electMaster():
    lower_limit = request.get_json()['lower_limit']
    upper_limit = request.get_json()['upper_limit']
    return Response(random.randint(lower_limit, upper_limit),status='200')

if __name__ == '__main__':
    app.run(host="localhost", port=int(sys.argv[1]), debug=True)
