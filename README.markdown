EmailReplyParser
================

**EmailReplyParser** is a small Java library for parsing plain text email content, based on GitHub's [email_reply_parser](http://github.com/github/email_reply_parser) library.


Installation
------------

Run `mvn clean test package`. A nice and fresh `EmailReplyParser-1.0.jar` file will appear in the `EmailReplyParser/target` directory.


Example Usage
-------------

Import the Email and EmailParser classes.

```java
import com.edlio.emailreplyparser.Email;
import com.edlio.emailreplyparser.EmailParser;
```

Instantiate an `EmailParser` object and parse your email:

``` java
EmailParser parser = new EmailParser();
Email email = parser.parse(emailString);
```

You get an `Email` object that contains a set of `Fragment` objects. The `Email`
class exposes two methods:

* `getFragments()`: returns a list of fragments;
* `getVisibleText()`: returns a string which represents the content considered as "visible".
* `getHiddenText()`: returns a string which represents the content considered as "hidden".

The `Fragment` represents a part of the full email content, and has the following API:

```java
String content = fragment.getContent();

boolean isSignature = fragment.isSignature();

boolean isQuoted = fragment.isQuoted();

boolean isHiiden = fragment.isHidden();

boolean isEmpty = fragment.isEmpty();
```

Alternatively, you can rely on the `EmailReplyParser` to either parse an email or get its visible content in a single line of code:

```java
Email email = EmailReplayParser.read(emailContentString);

String reply = EmailReplyParser.parseReply(emailContentString);
```


Credits
-------

* GitHub
* William Durand <william.durand1@gmail.com>
* Edlio


License
-------

EmailReplyParser is released under the MIT License. See the bundled LICENSE file for details.
