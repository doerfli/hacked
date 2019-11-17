# Privacy Policy

This page described how the _Hacked? - have i been pwned?_ app handles privacy and what happens to the data you enter in the app. 

## General

The _Hacked? - have i been pwned?_ app uses the service _Have i been pwned_ as it sole datasource. 
_Have i been pwned_ publishes its own privacy policy at [https://haveibeenpwned.com/Privacy](https://haveibeenpwned.com/Privacy) 

All data transmitted over the internet is sent over HTTPS connections. 

## When you save an email address in the app

Any email address entered in the app is stored within a local database on the device. 
The [Android Sandbox](https://source.android.com/security/app-sandbox) makes sure that only the app can access this database. 
The list is not sent anywhere, except when searching for breached accounts. 

## When you search for a breached account

When you search for an email address in the app, it sends the address to the API of _Have I Been Pwned_ via the _hibp-proxy_. 
The _hibp-proxy_ is required to supplement the request with the access key required for accessing the _Have i been pwned_ API. 
The _hibp-proxy_ does not explicitly store the email address in any persistent data storage, it only forwards the request to the _Have I Been Pwned API service_ and returns the response, which does not contains the email address any more. 
The response is returned to the device through (Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging), a messaging solution provided by Google. 


## When you check your password

The _pwned password_ function checks a user-provided password against a list of known breached passwords. 
The password is not sent in cleartext to any service what so ever. 
Instead it is hashed on the device and only the first 5 characters of the hash are sent to _Have I Been Pwned API_. 
This process is called _k-Anonymity_ and more details are provided in this [article](https://blog.cloudflare.com/validating-leaked-passwords-with-k-anonymity/).
The request is sent directly to the _Have I Been Pwned API_ and is not sent through the _hibp-proxy_. 

## Logging

The app stores limited logs through the Android Log service. 
These logs are stored within the Android device and never sent to an external system. 

The _hibp-proxy_ stores only the bare minimum logs keep the service operational and combat malicious activity. 
This includes transient web server logs. 
These logs may include the data entered by the user, browser headers such as the user agent string and in some cases, the user's IP address. 

## Hosting

The app itself requires no hosting at runtime. The _hibp-proxy_ service is hosted in Heroku's europe data center. 

## Source code

The source code for [_Hacked? - have i been pwned_](https://github.com/doerfli/hacked) and the [_hibp-proxy_](https://github.com/doerfli/hibp-proxy) can be found on their respective Github pages. 

