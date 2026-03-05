const sleep = (delay) => new Promise((resolve) => setTimeout(resolve, delay));

const sibsocket = new WebSocket('ws://127.0.0.1:1898');

sibsocket.onopen = async function (event) {
    sibsocket.send(JSON.stringify({
        "message": "connect",
        "clientName": "NameOfYourApplication",
        "handshakeVersion": "1.0",
        // "plugins": ["plugin1", "plugin2", "plugin3"], // optional 
        // or 
        "sessionToken": "{17fcce6b-4d64-4154-8cd9-4bf68ce75703}" // optional
    }));
    await sleep(1000)

    sibsocket.send(JSON.stringify({
        "message": "invokeCommands",
        "commands": ["arpeggio"]
    }))
}

sibsocket.onmessage = function (event) {
    var jsonObj = JSON.parse(event.data);
    console.log(jsonObj);
}

// sessionToken: '{17fcce6b-4d64-4154-8cd9-4bf68ce75703}'
