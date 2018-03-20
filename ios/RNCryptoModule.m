
#import "RNCryptoModule.h"
#import "RSAFormatter.h"

#define RSA_KEY_SIZE @4096
#define SEC_RANDOM_LENGTH 32

typedef void (^SecKeyPerformBlock)(SecKeyRef key);

@implementation RNCryptoModule

#pragma mark - Protocol implementation

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

#pragma mark - React Native methods

RCT_EXPORT_MODULE(Crypto)

RCT_REMAP_METHOD(getKeyStoreIsLoaded,
                 getKeyStoreIsLoadedWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(@YES);
}

RCT_REMAP_METHOD(getKeyAliases,
                 getKeyAliasesWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSMutableArray *aliases = [NSMutableArray array];

    // search for keys & return attributes
    NSDictionary *query = @{(id)kSecReturnAttributes    : @YES,
                            (id)kSecMatchLimit          : (id)kSecMatchLimitAll,
                            (id)kSecClass               : (id)kSecClassKey
                            };
    CFTypeRef result = NULL;
    OSStatus rc = SecItemCopyMatching((__bridge CFDictionaryRef)query, &result);

    if (rc == errSecSuccess) {
        // enumerate results
        [(__bridge NSArray*)result enumerateObjectsUsingBlock:^(id  _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            NSString *key = [obj objectForKey:(__bridge id)kSecAttrLabel];
            if (key) {
                [aliases addObject:key];
            }
        }];

        // convert back to immutable
        resolve([NSArray arrayWithArray:aliases]);
    } else {
        reject([NSString stringWithFormat:@"%d", rc], @"Error accessing keychain", nil);
    }

    if (result != NULL) CFRelease(result);
}

RCT_REMAP_METHOD(containsKeyAlias,
                 containsKeyAlias:(NSString*)alias
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *query = @{(id)kSecReturnAttributes    : @YES,
                            (id)kSecMatchLimit          : (id)kSecMatchLimitOne,
                            (id)kSecClass               : (id)kSecClassKey,
                            (id)kSecAttrLabel           : alias
                            };
    CFTypeRef result = NULL;
    OSStatus rc = SecItemCopyMatching((__bridge CFDictionaryRef)query, &result);
    NSLog(@"found key for alias %@: %@", alias, (__bridge id)result);
    if (result != NULL) CFRelease(result);

    if (rc == errSecSuccess || rc == errSecItemNotFound) {
        resolve([NSNumber numberWithBool:rc != errSecItemNotFound]);
    } else {
        reject([NSString stringWithFormat:@"%d", rc], @"Error accessing keychain", nil);
    }
}

RCT_REMAP_METHOD(deleteKeyEntry,
                 deleteKeyEntry:(NSString*)alias
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    NSDictionary *query = @{(id)kSecClass               : (id)kSecClassKey,
                            (id)kSecAttrLabel           : alias
                            };

    OSStatus rc = SecItemDelete((CFDictionaryRef)query);
    if (rc == errSecSuccess) {
        resolve(nil);
    } else {
        reject([NSString stringWithFormat:@"%d", rc], @"Error accessing keychain", nil);
    }
}

RCT_REMAP_METHOD(getKeyStoreCertificate,
                 getKeyStoreCertificate:(NSString *)alias
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    reject(@"-1", @"Unsupported on iOS", nil);
}

RCT_REMAP_METHOD(keyStoreSize,
                 keyStoreSizeWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    reject(@"-1", @"Unsupported on iOS", nil);
}

RCT_REMAP_METHOD(loadKeyStore,
                 loadKeyStoreWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(nil);
}

RCT_REMAP_METHOD(unloadKeyStore,
                 unloadKeyStoreWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    resolve(nil);
}

RCT_REMAP_METHOD(sign,
                 sign:(NSString*)dataString alias:(NSString*)alias
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    void(^signer)(SecKeyRef) = ^(SecKeyRef privateKey) {

        if (privateKey == NULL) {
            reject(@"-1", @"Signing error", nil);
            return;
        }
        SecKeyAlgorithm algorithm = kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256;

        BOOL canSign = SecKeyIsAlgorithmSupported(privateKey,
                                                  kSecKeyOperationTypeSign,
                                                  algorithm);

        NSData* signature = nil;

        if (canSign) {
            CFErrorRef error = NULL;
            NSData* messageBytes = [dataString dataUsingEncoding:NSUTF8StringEncoding];
            signature = (NSData*)CFBridgingRelease(SecKeyCreateSignature(privateKey,
                                                                         algorithm,
                                                                         (__bridge CFDataRef)messageBytes,
                                                                         &error));
            if (!signature) {
                NSError *err = CFBridgingRelease(error);
                NSLog(@"error: %@", err);
                reject(@"-1", @"Signing error", err);
            } else {
                NSString* encodedSignature = [signature base64EncodedStringWithOptions:NSDataBase64Encoding64CharacterLineLength];
                resolve(encodedSignature);
            }
        } else {
            reject(@"-1", @"Unsupported algorithm", nil);
        }
    };

    [self performWithPrivateKeyLabel:alias block:signer];
}

RCT_REMAP_METHOD(verify,
                 verify:(NSString*)signature alias:(NSString*)alias
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject)
{
    reject(@"-1", @"Unsupported on iOS", nil);
}

RCT_REMAP_METHOD(getKeyAsPem,
                 getKeyAsPem:(NSString*)alias
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock) reject)
{
    reject(@"-1", @"Unsupported on iOS", nil);
}

RCT_REMAP_METHOD(addKeyPair,
                 addKeyPair:(NSString*)label certificateFilename:(NSString*)filename
                 withResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock) reject)
{
    NSDictionary *query = @{(id)kSecReturnAttributes    : @YES,
                            (id)kSecMatchLimit          : (id)kSecMatchLimitOne,
                            (id)kSecClass               : (id)kSecClassKey,
                            (id)kSecAttrLabel           : label
                           };

    CFTypeRef result = NULL;
    OSStatus rc = SecItemCopyMatching((__bridge CFDictionaryRef)query, &result);
    if (rc == errSecItemNotFound) {
        NSDictionary *privateKeyAttributes = @{(id)kSecAttrIsPermanent  : @YES,
                                               (id)kSecAttrLabel        : label
                                               };

        NSDictionary *attributes = @{ (id)kSecAttrKeyType       : (id)kSecAttrKeyTypeRSA,
                                      (id)kSecAttrKeySizeInBits : RSA_KEY_SIZE,
                                      (id)kSecPrivateKeyAttrs   : privateKeyAttributes
                                      };

        CFErrorRef error = NULL;
        SecKeyRef privateKey = SecKeyCreateRandomKey((__bridge CFDictionaryRef)attributes, &error);

        if (!privateKey) {
            NSError *err = CFBridgingRelease(error);
            NSLog(@"%@", err);
            reject(@"-1", @"Error generating RSA keypair", err);
        } else {
            SecKeyRef publicKey = SecKeyCopyPublicKey(privateKey);
            NSString *publicKeyPem = [self externalRepresentationForPublicKey:publicKey];
            resolve(@{@"publicKeyPem": publicKeyPem});
        }
    } else {
        reject(@"-1", [NSString stringWithFormat: @"Private key with label '%@' already exists", label], nil);
    }
}

RCT_REMAP_METHOD(secureRandom,
                 secureRandomWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock) reject)
{

    NSMutableData *keyBytes = [[NSMutableData alloc] initWithLength:SEC_RANDOM_LENGTH];
    OSStatus rc = SecRandomCopyBytes(kSecRandomDefault, keyBytes.length, keyBytes.mutableBytes);
    if (rc == errSecSuccess) {
        NSString *result = [keyBytes base64EncodedStringWithOptions:0];
        resolve(result);
    } else {
        reject([NSString stringWithFormat:@"%d", rc], @"Error generating secure random", nil);
    }
}

#pragma mark - Helper methods

- (void)performWithPrivateKeyLabel:(NSString *)label block:(SecKeyPerformBlock)performBlock
{
    NSDictionary *query = @{ (id)kSecClass      : (id)kSecClassKey,
                             (id)kSecAttrLabel  : label,
                             (id)kSecReturnRef  : @YES
                             };

    SecKeyRef key = NULL;
    OSStatus status = SecItemCopyMatching((__bridge CFDictionaryRef)query, (CFTypeRef *)&key);

    if (status != errSecSuccess) {
        NSLog(@"error accessing the key");
        if (performBlock) { performBlock(NULL); }
    } else {
        if (performBlock) { performBlock(key); }
        if (key) { CFRelease(key); }
    }
}

- (void)performWithPublicKeyLabel:(NSString *)label block:(SecKeyPerformBlock)performBlock
{
    [self performWithPrivateKeyLabel:label block:^(SecKeyRef key) {
        SecKeyRef publicKey = SecKeyCopyPublicKey(key);

        if (performBlock) { performBlock(publicKey); }
        if (publicKey) { CFRelease(publicKey); }
    }];
}

- (NSString *) externalRepresentationForPublicKey:(SecKeyRef)key
{
    NSData *keyData = [self dataForKey:key];
    return [RSAFormatter PEMFormattedPublicKey:keyData];
}

- (NSString *) externalRepresentationForPrivateKey:(SecKeyRef)key
{
    NSData *keyData = [self dataForKey:key];
    return [RSAFormatter PEMFormattedPrivateKey:keyData];
}

- (NSData *)dataForKey:(SecKeyRef)key
{
    CFErrorRef error = NULL;
    NSData * keyData = (NSData *)CFBridgingRelease(SecKeyCopyExternalRepresentation(key, &error));

    if (!keyData) {
        NSError *err = CFBridgingRelease(error);
        NSLog(@"%@", err);
    }

    return keyData;
}

@end
  
