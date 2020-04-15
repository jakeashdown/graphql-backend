lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.4"

Compile/mainClass := Some("com.heavens_above.QuickstartApp")

graphqlSchemaSnippet := "com.heavens_above.user.UserSchema.schema"

target in graphqlSchemaGen := target.value / "graphql-build-schema"

lazy val root = (project in file("."))
  .enablePlugins(GraphQLSchemaPlugin, GraphQLQueryPlugin)
  .settings(
    inThisBuild(List(
      organization    := "com.heavens-above",
      scalaVersion    := "2.13.1"
    )),
    name := "graphql-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "org.sangria-graphql" %% "sangria" % "1.4.2",
      "org.sangria-graphql" % "sangria_2.13" % "2.0.0-M4",
      "org.sangria-graphql" % "sangria-spray-json_2.13" % "1.0.2",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test
    )
  )
