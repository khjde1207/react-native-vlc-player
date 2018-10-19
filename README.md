# react-native-vlc-player

A `<VLCPlayer>` component for react-native

Based on [react-native-vlcplayer](https://github.com/xiongchuan86/react-native-vlcplayer) from [xiongchuan86](https://github.com/xiongchuan86) and on [react-native-vlc-player](https://github.com/ghondar/react-native-vlc-player) from [ghondar](https://github.com/ghondar)

Updated for iOS 12, Android 6.0+ using the latest VLCKit 3.x

### Add it to your project

Run `npm i -S https://github.com/wfilleman/react-native-vlc-player.git`

#### iOS

- add `pod 'MobileVLCKit', '3.1.4'` in your Podfile
- `pod install`
- `react-native link react-native-vlc-player`
- also you must disable bitcode option in target build settings (otherwise it not linked correctly for armv7)
- also you must add `libstdc++.6.0.9.tbd` to `Linked Framework and Libraries`

#### Android

- in settings.gradle change:

  `new File(rootProject.projectDir, '../node_modules/react-native-vlc-player/android')
  `
  

  to

  `new File(rootProject.projectDir, '../node_modules/react-native-vlc-player/android/vlc')`
  
  add
  `
   include ':libvlc'
  
  project(':libvlc').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-vlc-player/android/libvlc')
  
  `
  
- in *MainApplication.java* you need to import `com.rusmigal.vlcplayer.VLCPlayerPackage` instead of `com.vlcplayer.VLCPlayerPackage`

## Usage
 

```
state = {paused : false, uri : ""}
constructor(props) {
    super(props); 
    this.state.uri = "rtsp://...."
    
}
onProgress(){

}
onEnded(){

}
onPlaying(){

}
onPaused(){

}
onBuffering(){

}
render() {
    if(this.state.uri == ""){
      return (<View></View>)
    }
    <VLCPlayer
        ref='vlcplayer'
        paused={this.state.paused}
        style={styles.vlcplayer}
        source={{uri: this.state.uri, initOptions: ['--codec=avcodec',"--rtsp-tcp"] , "autoplay":true}}
        onVLCProgress={this.onProgress.bind(this)}
        onVLCEnded={this.onEnded.bind(this)}
        onVLCStopped={this.onEnded.bind(this)}
        onVLCPlaying={this.onPlaying.bind(this)}
        onVLCBuffering={this.onBuffering.bind(this)}
        onVLCPaused={this.onPaused.bind(this)}
     />
 }

```
### Properties
source.initOptions - only for iOS
rate - only for iOS
snapshotPath - only for iOS

### Callbacks
onBuffering
onPlaying
onStopped

## Static Methods

`seek(seconds)`

```
this.refs['vlcplayer'].seek(0.333);
```

`snapshot(path)`

```
this.refs['vlcplayer'].snapshot(path);
```
