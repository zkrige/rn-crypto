
# react-native-crypto-module

## Getting started

`$ npm install react-native-crypto-module --save`

### Mostly automatic installation

`$ react-native link react-native-crypto-module`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-crypto-module` and add `RNCryptoModule.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNCryptoModule.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.reactlibrary.RNCryptoModulePackage;` to the imports at the top of the file
  - Add `new RNCryptoModulePackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-crypto-module'
  	project(':react-native-crypto-module').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-crypto-module/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-crypto-module')
  	```

#### Windows
[Read it! :D](https://github.com/ReactWindows/react-native)

1. In Visual Studio add the `RNCryptoModule.sln` in `node_modules/react-native-crypto-module/windows/RNCryptoModule.sln` folder to their solution, reference from their app.
2. Open up your `MainPage.cs` app
  - Add `using Crypto.Module.RNCryptoModule;` to the usings at the top of the file
  - Add `new RNCryptoModulePackage()` to the `List<IReactPackage>` returned by the `Packages` method


## Usage
```javascript
import RNCryptoModule from 'react-native-crypto-module';

// TODO: What to do with the module?
RNCryptoModule;
```
  