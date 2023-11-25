# Name: Saksham Khatod
# Police Thief Game

This project uses Akka HTTP to create an API that allows users
to play a simple graph game.

To run the project, make sure you have sbt, scala, jdk installed on your machine.
This project uses scala version 2.13.12, sbt version 1.9.6 & jdk from openjdk 11.0.20.1 2023-08-24.
This project includes 6 tests built using scalatest.

If you want to deploy this project to AWS EC2, uncomment code blocks with AWS EC2 written on the top and remove variables with the same names.

Each file is filled with comments that explain the methods used for the project.

To run the project, navigate to the project directory and run the following.

```
sbt clean compile test run
```

To build the project for AWS EC2, run the folllowing
```
sbt clean compile stage
```

Here's a youtube video link that explains the process of building this project and deploying it on AWS EC2 : https://youtu.be/kcqYeBlO86k

## API Guide
To start the game, first initialize both police & thief.
This game supports two strategies i.e. Safe & Random. Both are explained in detail in Strategies file.
You can execute a strategy for only one player or both of them.

To reset the game, run :-

    curl http://localhost:8080/restart

### Police
Initializing Police :

    curl http://localhost:8080/police

Find Next Possible Moves for Police:

    curl http://localhost:8080/police/possibleMoves

The output is in the form of Map with keys as node ids & values as confidence scores.

Find Distance to the Closest Valuable Node:

    curl http://localhost:8080/police/findValuableNode

Find Thief:

    curl http://localhost:8080/police/findThief

Move to a particular node:

    curl http://localhost:8080/police/move/{node_id}

Execute a Strategy:

    curl http://localhost:8080/police/strategy/safe
    curl http://localhost:8080/police/strategy/random

Check result after executing a strategy:

    curl http://localhost:8080/police/result

### Thief
Initializing Thief :

    curl http://localhost:8080/thief

Find Next Possible Moves for Thief:

    curl http://localhost:8080/thief/possibleMoves

The output is in the form of Map with keys as node ids & values as confidence scores.

Find Distance to the Closest Valuable Node:

    curl http://localhost:8080/thief/findValuableNode

Find Police:

    curl http://localhost:8080/thief/findPolice

Move to a particular node:

    curl http://localhost:8080/thief/move/{node_id}

Execute a Strategy:

    curl http://localhost:8080/thief/strategy/safe
    curl http://localhost:8080/thief/strategy/random

Check result after executing a strategy:

    curl http://localhost:8080/thief/result

### Strategies Result
In my tests, safe strategy finished the game much quicker by for e.g. letting the thief 
find valuable node, letting the police find the thief, going to a disjoint node, etc.
Random strategy, more often than not, results in much longer running times.
Even after making sure that both strategies prioritize unseen nodes over seen ones, 
safe strategy always wins in finishing the game quicker.

Of course, this game allows for both police & thief to choose a different strategy.
In these cases, the game's execution time is hard to predict.

