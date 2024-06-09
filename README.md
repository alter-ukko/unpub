## unpub

Unpub is web service for reading books via the web browser.

The reason it exists is interesting. I wanted to use an e-book reader on iOS that allowed me to override the font to one of my liking. In the process of trying to do that, I learned that the only way to install a system-wide font on iOS was via a configuration profile. These are usually used by businesses to control the devices of their employees, but if you're a normal user trying to get a custom font, it means that you have to install a third-party app that downloads fonts and builds a configuration profile, which you then have to approve to be installed on your device.

No kidding, this is how it works. Obviously configuration profiles installed by third-party apps are an excellent vector for malware, so I didn't really want to go that route.

It got me thinking, though. If you browsed to a web page on your iOS device, that web page could display with any font it wanted. Seems unfair, especially since epub files are really just zip files with a bunch of web pages inside.

Which also got me thinking. We have come to a place where we've turned applications into web pages (Electron, SPAs) and web pages into apps (e-book readers). And we're seemingly totally fine with that.

Anyway, I decided to write a program that converted all my epub files into single web pages, discarding all the style information. And then all I needed was a web server to serve out those files and also let you change the styles in the basic ways you would with an e-book reader.

So that's what unpub does. I haven't decided yet whether to implement some sort of page-turning viewer. I probably won't since it kind of defeats the purpose of just having a book as a web page. But a few things I will probably want are:

* The ability to bookmark where you are in a book and go there.
* Authentication, because we can't have anything nice.

This isn't really meant to be a multi-user web service. It's more of a personal web service that you'd install somewhere where you have permissions to write to the file system. I've always kind of liked the idea of personal web services.