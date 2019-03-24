Originally forked from [deano2390/MaterialShowcaseView](https://github.com/deano2390/MaterialShowcaseView). I've used this library for creating a Education Framework for Newly Onboarded Users in [Zeta Android App](https://play.google.com/store/apps/details?id=in.zeta.android).

### Changes that I've done:

- New type of elements:
  - Spotlight : a view in the center, on top of the current view hierarchy which disables background touches
  - Tour : a viewpager tour at the bottom
  - Notification : a view at the top with option to dismiss
- Added listener support for elements so that consumer app can act on it.
- Added flexibility for consumer to add it's own textviews.
- Refactored a lot of things (removed hungarian naming convention ! ) 
- Some UI tweaks such as support for higlighting.


### Blog
I've also written a blog explaining how the original Showcase library works : https://medium.com/@yashgirdhar/android-material-showcase-view-part-1-22abd5c65b85. Do have a look and let me know if you have any comments.

Feel free to ping me in case of any doubts.



# MaterialShowcaseView
A Material Design themed ShowcaseView for Android

This library is heavily inspired by the original [ShowcaseView library][1].

Since Google introduced the Material design philosophy I have seen quite a few apps with a nice clean, flat showcase view (the Youtube app is a good example). The only library out there however is the [original one][1]. This was a great library for a long time but the theming is now looking a bit dated.

![Logo](http://i.imgur.com/QIMYRJh.png)


![Animation][2]

# Gradle
--------

[![jitpack][4]][5]

Add the jitpack repo to your your project's build.gradle at the end of repositories [Why?](#why-jitpack)

/build.gradle
```groovy
allprojects {
	repositories {
		jcenter()
		maven { url "https://jitpack.io" }
	}
}
```

Then add the dependency to your module's build.gradle:

/app/build.gradle
```groovy
compile 'com.github.deano2390:MaterialShowcaseView:1.1.0'
```

NOTE: Some people have mentioned that they needed to add the @aar suffix to get it to resolve from JitPack:
```groovy
compile 'com.github.deano2390:MaterialShowcaseView:1.1.0@aar'
```

# How to use
--------
This is the basic usage of a single showcase view, you should check out the sample app for more advanced usage.

```java

	// single example
	new MaterialShowcaseView.Builder(this)
                .setTarget(mButtonShow)
                .setDismissText("GOT IT")
                .setContentText("This is some amazing feature you should know about")
                .setDelay(withDelay) // optional but starting animations immediately in onCreate can make them choppy
                .singleUse(SHOWCASE_ID) // provide a unique ID used to ensure it is only shown once
                .show();
                
                
                
                
    	// sequence example            
    	ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(500); // half second between each showcase view

        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, SHOWCASE_ID);

        sequence.setConfig(config);

        sequence.addSequenceItem(mButtonOne,
                "This is button one", "GOT IT");

        sequence.addSequenceItem(mButtonTwo,
                "This is button two", "GOT IT");

        sequence.addSequenceItem(mButtonThree,
                "This is button three", "GOT IT");

        sequence.start();
                
```

# Why Jitpack
------------
Publishing libraries to Maven is a chore that takes time and effort. Jitpack.io allows me to release without ever leaving GitHub so I can release easily and more often.


# License
-------

    Copyright 2015 Dean Wild

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.





[1]: https://github.com/amlcurran/ShowcaseView
[2]: http://i.imgur.com/rFHENgz.gif
[3]: https://code.google.com/p/android-flowtextview/
[4]: https://img.shields.io/github/release/deano2390/MaterialShowcaseView.svg?label=JitPack
[5]: https://jitpack.io/#deano2390/MaterialShowcaseView
