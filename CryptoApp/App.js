/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 * @flow
 */

import React, { Component } from 'react';
import {
  Platform,
  StyleSheet,
  Text,
  View
} from 'react-native';


import { Crypto } from 'react-native-crypto-module';

const instructions = Platform.select({
  ios: 'Press Cmd+R to reload,\n' +
    'Cmd+D or shake for dev menu',
  android: 'Double tap R on your keyboard to reload,\n' +
    'Shake or press menu button for dev menu',
});

class GetKeyStoreIsLoadedTest extends Component<Props> {
  constructor(props) {
    super(props)
    this.state = { loaded: false }
  }

  componentDidMount() {
    var self = this
    return Crypto.getKeyStoreIsLoaded().then(loaded => {
      self.setState({loaded: loaded})
    })
  }

  render() {
    var testResult = "getKeyStoreIsLoaded =  " + this.state.loaded
    return (
      <Text style={styles.test}>
        {testResult}
      </Text>
    )
  }
}

class SecureRandomTest extends Component<Props> {
  constructor(props) {
    super(props)
    this.state = { secureRandom: "" }
  }
  
  componentDidMount() {
    var self = this
    Crypto.secureRandom().then(random => {
        self.setState({secureRandom: random})
    })
  }
  
  render() {
    var testResult = "secureRandom = " + this.state.secureRandom
    return (
      <Text style={styles.test}>
        {testResult}
      </Text>
    )
  }
}

class AddKeyPairTest extends Component<Props> {
  constructor(props) {
    super(props)
    this.state = { alias: '', keypem: '' }
  }
  
  componentDidMount() {
    var self = this
    Crypto.addKeyPair(this.props.alias, '').then(keypem => {
      self.setState({ alias: self.props.alias, keypem: keypem.publicKeyPem })
      Crypto.deleteKeyEntry(self.props.alias).then(() => {
        console.log("test key '" + self.props.alias + "' removed after test")
      })
    })
  }
  
  render() {
    var testResult = "addKeyPair alias = " + this.state.alias
    return (
      <Text style={styles.test}>
        {testResult}
      </Text>
    )
  }
}

class KeyAliasesTest extends Component<Props> {
  constructor(props) {
    super(props)
    this.state = { aliases: [], containsCreatedKey: false }
  }
  
  componentDidMount() {
    var self = this
    Crypto.addKeyPair(self.props.alias, '').then(keypem => {
      Crypto.containsKeyAlias(self.props.alias).then(
        contains => {
          if (contains) {
            Crypto.getKeyAliases().then(aliases => {
              self.setState({ aliases: aliases, containsCreatedKey : contains })
              Crypto.deleteKeyEntry(self.props.alias).then(() => {
                console.log("test key '" + self.props.alias + "' removed after test")
              })
            })
          } else {
            console.log("WARNING: Crypto.containsKeyAlias returned false")
          }
        }
      )
    })
  }
  
  render() {
    var testResult = "getKeyAliases = " + this.state.aliases
    return (
      <Text style={styles.test}>
        {testResult}
      </Text>
    )
  }
}

class SignTest extends Component<Props> {
  constructor(props) {
    super(props)
    this.state = { signature: '' }
  }
  
  componentDidMount() {
    var self = this
    Crypto.addKeyPair(self.props.alias, '').then(keypem => {
      Crypto.containsKeyAlias(self.props.alias).then(contains => {
        if (contains) {
          Crypto.sign(self.props.message, self.props.alias).then(signature => {
            self.setState({ signature: signature })
            Crypto.deleteKeyEntry(self.props.alias).then(() => {
              console.log("test key '" + self.props.alias + "' removed after test")
            })
          })
        } else {
          console.log("WARNING: Crypto.containsKeyAlias returned false")
        }
      })
    })
  }
  
  render() {
    var testResult = "sign message '" + this.props.message + "' = " + this.state.signature
    return (
      <Text style={styles.test}>
        {testResult}
      </Text>
    )
  }
}

type Props = {};
export default class App extends Component<Props> {
  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>
          Testing keystore methods
        </Text>
        <GetKeyStoreIsLoadedTest/>
        <SecureRandomTest/>
        <AddKeyPairTest alias="testkey1"/>
        <AddKeyPairTest alias="testkey2"/>
        <KeyAliasesTest alias="testkey3"/>
        <SignTest alias="testkey4" message="test message to sign"/>
        <Text style={styles.instructions}>
          {instructions}
        </Text>
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'flex-start',
    alignItems: 'flex-start',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    marginLeft: 10,
    marginTop: 30,
    marginBottom: 10,
  },
  instructions: {
    margin: 10,
    color: '#333333',
    marginBottom: 5,
    marginTop: 5,
  },
  test: {
    marginLeft: 10,
    color: '#333333',
    fontSize: 9,
  },
});
