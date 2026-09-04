# Privacy Policy

This page describes how the _Hacked? - have i been pwned?_ app handles privacy and what happens to the data you enter in the app.

## General

The _Hacked? - have i been pwned?_ app uses the _Have I Been Pwned_ service as its sole data source.
_Have I Been Pwned_ publishes its own privacy policy at [https://haveibeenpwned.com/Privacy](https://haveibeenpwned.com/Privacy).

All data transmitted over the internet is sent over HTTPS connections.

## When you save an email address in the app

Any email address entered in the app is stored in a local database on the device.
The [Android sandbox](https://source.android.com/security/app-sandbox) ensures that only the app can access this database.
The list is never sent anywhere, except when searching for breached accounts.

## When you search for a breached account

When you search for an email address in the app, it is sent to the _Have I Been Pwned_ API via the _hibp-proxy_.
The _hibp-proxy_ supplements the request with the access key required to use the _Have I Been Pwned_ API.
It does not store the email address in any persistent storage — it only forwards the request to the _Have I Been Pwned_ API and returns the response, which no longer contains the email address.
The response is delivered to the device via [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging), a messaging service provided by Google.

## When you check your password

The _pwned password_ function checks a password you provide against a list of known breached passwords.
The plain-text password is never sent to any service.
Instead, it is hashed on the device, and only the first 5 characters of the hash are sent to the _Have I Been Pwned_ API.
This process is called _k-Anonymity_; more details are available in [this article](https://blog.cloudflare.com/validating-leaked-passwords-with-k-anonymity/).
This request goes directly to the _Have I Been Pwned_ API, not through the _hibp-proxy_.

## Logging

The app stores limited technical logs via the Android Log service.
These logs stay on the device and are never sent to an external system.

If the app crashes, a crash report is sent to [Crashlytics](https://firebase.google.com/docs/crashlytics) for analysis.

The app uses [Firebase Analytics](https://firebase.google.com/docs/analytics) to track a small number of key events (e.g. account added, password checked, breach acknowledged). No content data is ever sent to the analytics service — only the fact that an event occurred. This data is used to understand how the app is used.

The _hibp-proxy_ keeps only the bare minimum logs needed to keep the service running and to combat malicious activity. This includes transient web server logs, which may include data entered by the user and, in some cases, the user's IP address.

## Hosting

The app itself requires no hosting. The _hibp-proxy_ service is hosted on a server in Germany.

## Source code

The source code for [Hacked? - have i been pwned](https://github.com/doerfli/hacked) and [hibp-proxy](https://github.com/doerfli/hibp-proxy) is available on their respective GitHub pages.
