# network-code-coloring

This project uses [Gradle](https://gradle.org/).
To build and run the application, use the *Gradle* tool window by clicking the Gradle icon in the right-hand toolbar,
or run it directly from the terminal:

* Run `./gradlew run` to build and run the application.
* Run `./gradlew build` to only build the application.
* Run `./gradlew check` to run all checks, including tests.
* Run `./gradlew clean` to clean all build outputs.

Note the usage of the Gradle Wrapper (`./gradlew`).
This is the suggested way to use Gradle in production projects.

[Learn more about the Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

[Learn more about Gradle tasks](https://docs.gradle.org/current/userguide/command_line_interface.html#common_tasks).

This project follows the suggested multi-module setup and consists of the `app` and `utils` subprojects.
The shared build logic was extracted to a convention plugin located in `buildSrc`.

This project uses a version catalog (see `gradle/libs.versions.toml`) to declare and version dependencies
and both a build cache and a configuration cache (see `gradle.properties`).

## Design notes
* Есть ноды, они могут быть запущены на физически разных машинах. У каждой ноды есть пул инстансов классов
* Будем называть такие классы акторами
* Методы актора могут быть вызваны remote нодами
* К записи полей актора имеет прямой доступ только та нода, в пуле которой находится этот актор
* Ноды позволяют другим нодам создавать новых актором в их пуле

## Questions
* Rpc requires suspend modifier. Does it mean that I have to use suspend modifier everywhere?
* Getters and constructors cannot have suspend modifiers
* На каких платформах поддерживается рефлексия на нужном мне уровне?
* Неясно, как сериализовывать и десериализовывать `Any`
* Вероятно, придётся отказаться от kotlin rpc
  * Kotlin RPC не позволяет работать с Any автоматически, поэтому если использовать примитивы call и spawn, 
    то нужно будет делать часть сериализации самому. Тогда кажется, что уж лучше просто не использовать kotlin RPC и
    всё делать самому
  * Ещё один подход это генерировать интерфейс с кучей методов для kotlin rpc, чтобы на каждый call был свой метод. Однако
    kotlin rpc не позволяет использовать generics.

## Заметки
Я исхожу из принципа "чем меньше магии, тем лучше". Любую генерацию кода я считаю магией. Поэтому я пытаюсь сделать так, чтобы
пришлось генерировать как можно меньше кода.

