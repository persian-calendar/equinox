# Equinox
[![](https://jitpack.io/v/persian-calendar/calendar.svg)](https://jitpack.io/#persian-calendar/equinox)

Ported from Calendar package for Go https://github.com/xyproto/calendar/blob/master/equinox.go

```
The MIT License (MIT)

Copyright (c) 2017 Alexander F Rødseth

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

# Usage
### Gradle
Add it in your root build.gradle at the end of repositories:
```kotlin
allprojects {
    repositories {
        ...
        maven("https://jitpack.io")
    }
}
```  
Add the dependency
```kotlin
dependencies {
    implementation("com.github.persian-calendar:equinox:2.0.0")
}
```
 
for other build tools refer to [this](https://jitpack.io/#persian-calendar/calendar) documentation.
