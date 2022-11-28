import flask
from flask import request,jsonify
import requests
from flask import Flask, render_template,request,redirect,url_for 
from bson import ObjectId 
from pymongo import MongoClient 
import os

app = flask.Flask(__name__)
app.config["DEBUG"] = True

pubSubServer = ""

@app.route('/send', methods=['POST'])
def sendMessage():
    data = request.get_json(force=True)
    print(data)
    res = requests.post("http://"+pubSubServer+"/getMessage", json=data)
   
    return res.text

## Add Topics
@app.route('/addTopic', methods=['POST'])
def addTopic():
    data = request.get_json(force=True)
    res = requests.post("http://"+pubSubServer+"/addTopic", json=data)
    
    return res.text

@app.route('/addPublisher', methods=['POST'])
def addPublisher():
    data = request.get_json(force=True)
    print(data)
    res = requests.post("http://"+pubSubServer+"/addPublisher", json=data)
    
    return res.text

@app.route('/registerPubSubServer', methods = ['POST'])
def registerPubSubServer():
    global pubSubServer
    data = request.get_json(force=True)
    server = data['server']
    pubSubServer = server
    return "Successfully Registered"

    
    

if __name__ == '__main__':
    app.run(host="localhost", port=3000, debug=True)
