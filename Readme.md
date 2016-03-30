Yet Another Message Broker (yamb)
===
<a href="https://travis-ci.org/meledin/yamb"><img src="https://travis-ci.org/meledin/yamb.svg?branch=master"/></a>

This package contains yet another message broker (several, in fact), allowing experimentation on how an application would use messaging. The current main attraction is *amb*, the Abstract Message Broker API, which allows seamless transition between messaging backends. Meanwhile, *tmb* is an absolutely minimal message broker that nevertheless supports both native and web apps, and can be used as a baseline tool for doing integrated testing of messagebus-dependant apps. Combo it with amb to allow the deployment to scale to a real message bus and there's something there!