MessageBrokerClient = (config)=> {
    if (!config.status) config.status = (status)=> console.log("MessageBrokerClient: " + status)
    if (!config.url) config.url = "ws"+location.protocol.substr(4)+"//"+location.host+"/broker"
    var socket,
        reconnects = 0,
        scheduled,
        schedule = (millis)=> {
            if (!scheduled ) {
                scheduled = true
                setTimeout(connect, millis)
                if (millis > 3000)
                    tick(new Date(+new Date() + millis))
            }
        },
        tick = (until)=> {
            var seconds = Math.round((+until - +new Date()) / 1000);
            config.status("reconnect attempt " + reconnects + " in " + seconds + " seconds")
            if (seconds > 0) setTimeout(()=> tick(until), 1000)
        },
        connect = ()=> {
            scheduled = false
            if (reconnects > 90) reconnects = 30

            socket = new WebSocket(config.url)
            socket.onmessage = (m)=> config.handle(JSON.parse(JSON.parse(m.data).payload))
            socket.onclose = ()=> schedule(500)
            socket.onerror = ()=> schedule(2000 * ++reconnects)
            socket.onopen = ()=> {
                reconnects = 0
                config.status("online")
                config.connected && config.connected()
            }
        },
        send = (type, topic, payload)=> {
            socket.send(JSON.stringify({type:type, topic:topic, payload: payload ? JSON.stringify(payload) : undefined}))
        }
    connect()
    return {
        send: send
    }
}