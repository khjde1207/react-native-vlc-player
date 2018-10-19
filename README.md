# react-native-vlc-player

A `<VLCPlayer>` component for react-native

Based on [react-native-vlcplayer](https://github.com/xiongchuan86/react-native-vlcplayer) from [xiongchuan86](https://github.com/xiongchuan86) and on [react-native-vlc-player](https://github.com/ghondar/react-native-vlc-player) from [ghondar](https://github.com/ghondar)

Updated for iOS 12, Android 6.0+ using the latest VLCKit 3.x

### Add it to your project

Run `npm i -S https://github.com/khjde1207/react-native-vlc-player.git`

#### iOS

- add `pod 'MobileVLCKit', '3.1.4'` in your Podfile
- `pod install`
- `react-native link react-native-vlc-player`
- also you must disable bitcode option in target build settings (otherwise it not linked correctly for armv7)
- also you must add `libstdc++.6.0.9.tbd` to `Linked Framework and Libraries`

#### Android

- in settings.gradle change:

  ```
  
  include ':react-native-vlc-player'
  
project(':react-native-vlc-player').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-vlc-player/android')
```

  ```
  

  to

  ```
  include ':react-native-vlc-player'
  project(':react-native-vlc-player').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-vlc-player/android/vlc')
  ```
  
  
  
  add
  
  ```
  include ':libvlc'
  project(':libvlc').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-vlc-player/android/libvlc')
  
  ```
- in *MainActivity.java* remove `import com.rusmigal.vlcplayer.VLCPlayerPackage`
- in *MainApplication.java* add --
```
import com.rusmigal.vlcplayer.VLCPlayerPackage;
..
..

 @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
        +  new VLCPlayerPackage()
      );
    }

```


 android/app/build.gradle 
 
 ```
 dependencies{
 compile project(':react-native-vlc-player')
 ...
  
 
 }
 ```


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


이 밑으로 는 한글로 : 

해당 저장소의 존재 이유  :

1. 기존 저장소에선 옵션  설정 은 있지만 동작 하지 않는 버그가 있음,

2. readme 에 설명이 부족하여 동작 하기 까지 삽질이 너무 길어졌음.

3. android 버전을 메인 프로젝트 버전에 따라가도록 수정  - 참고 : https://github.com/react-native-community/react-native-svg

