# Cryptographic module for React Native

Most method implementations are borrowed from [react-native-rsa-native](https://github.com/amitaymolko/react-native-rsa-native/),
with some adjustments for a specific use case.

## Ported methods

* `getKeyStoreIsLoaded` - always returns true
* `getKeyAliases` - returns list of key aliases
* `containsKeyAlias` - indicates whether key alias already exists
* `deleteKeyEntry` - removes key pair
* `loadKeyStore` - does nothing (not applicable on iOS)
* `unloadKeyStore` - does nothing (not applicable on iOS)
* `sign` - signs a message using a given private key
* `addKeyPair` - generate and save a new key pair, returns public key PEM
* `secureRandom` - generate secure random (Base64 encoded)

## Methods not ported

* `getKeyStoreCertificate` - unused, not applicable on iOS
* `keyStoreSize` - unused, not applicable on iOS
* `verify` - unused, Android implementation is not valid
* `getKeyAsPem` - unused

## Notes

All methods are implemented asynchronously and return a promise

## Installation

Clone the repository, then

```
$ cd CryptoApp
$ react-native install ../CryptoModule # ignore error at the end of installation
$ react-native link react-native-crypto-module
```

