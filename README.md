# ParkourIO Plugin 

## ПЛАГИН ПРЕДНАЗНАЧЕН ТОЛЬКО ДЛЯ СЕРВЕРА ParkourIO !!!

## Установка
На данный момент для запуска плагина, необходимо в папке Plugins вручную создать папку `ParkourIO` и разместить в ней `config.json` и `parkour-levels.json`

## Пермишены
Все пермишены, как и метадаты (о них позже), начинаются с имени пакета - `ru.dfhub.parkourio` и `command`

`ru.dfhub.parkourio.command.reload` - Команда /reload
`ru.dfhub.parkourio.command.world` - Команда /world
`ru.dfhub.parkourio.command.metadata-util` - Команда /metadata-util
`ru.dfhub.parkourio.command.parkour` - Команда /parkour
`ru.dfhub.parkourio.command.spawn` - Команда /spawn

## Основные моменты 
Каждый заход игрока на сервер подразумевает:
- Телепортацию на спавн
- *(в будущем, метадаты для быстрой работы)*

## Устройство уровней паркура
Вся информация о уровнях хранится в `plugins/ParkourIO/parkour-levels.json` в виде массива из JSON-объектов следующего формата:
```json
[
  {
    "id" : 0,
    "spawn" : {
      "world" : "custom_or_default",
      "x" : 0.5,
      "y" : 0.5,
      "z" : 0.5,
      "yaw" : 0,
      "pitch" : 0
    },
    "fall-level" : 0,
    "start" : {
      "x" : 0,
      "y" : 0,
      "z" : 0
    },
    "end" : {
      "x" : 1,
      "y" : 1,
      "z" : 1
    },
    "checkpoints" : [
      {
        "world" : "world",
        "x" : 2.5,
        "y" : 2.5,
        "z" : 2.5,
        "yaw" : 0,
        "pitch": 0
      }
    ],
    "reward" : 10,
    "icon-item" : {
      "name" : "<green>Название уровня</green>",
      "lore" : [
        "<gray>Жесть, лооооооор</gray>",
        "Сколько угодно строк, только бы в экран поместилось"
      ],
      "material" : "RED_WOOL",
      "amount" : 1,
      "enchanted" : true
    }
  }
]
```

**!!! ВАЖНО !!!**
Во время работы плагина и, в особенности, прохождения паркура, информация о уровне будет часто искаться по id уровня, но в целях оптимизации в качестве `id` уровня берется **не id из json-объекта, а порядковый номер в массиве**, потому при создании новых уровней надо соблюдать следующие правила:
- Первый уровень с id = 0 
- Уровни идут в порядке возрастания id, сверху вниз в файле

<u>Любое несоблюдение одно из вышеперечисленных правил приведет к ошибкам</u>

### Уровни паркура в коде
Класс, который представляет из себя уровень - `ParkourLevel`, а утилита для работы с ними `ParkourLevels`.
Вся работа с уровнями работает не напрямую, а через утилиту. Чтобы перезагрузить список миров **из кода**, нужно вызвать метод `ParkourLevels.reload()`, но для данных целей есть команда `/preload levels`

Чтобы получито уровень по id, то можно использовать `ParkourLevels.getLevelById(int id)`, где id - номер желаемого уровня. Данный метод вернёт экземпляр класса `ParkourLevel`. При получении уровня нужно убедиться, что он загружен.

Для получения всех уровней, можно использовать `ParkourLevels.getLevels()`.

## Принцип работы уровней паркура

При попадании на паркур, игрок сразу получает 2 метадаты:
- `ru.dfhub.parkourio.parkour.on_parkour_level` (id паркура)
- `ru.dfhub.parkourio.parkour.checkpoint` (-1 - спавнавая точка)

Первая предоставляет возможность взаимодействовать с уровнем, т.е включает для игрока точки старта/чекпоинты/финиши, а также телепорт на чекпоинт при падении.

Вторая позволяет понять, в какой чекпоинт телепортировать игрока. В качестве значения указывается id чекпоинта. Принцип у них схож с id уровней паркура.

Обработчик движений не работает без этих метадат. При их наличии он считывает каждое движение игрока. Так как буквально на каждое движение игрока придется вызывать методы для проверки на точку старта/финиша/чекпоинтов, то, в качестве оптимизации, **проверяются только точки, где <u>на уровне головы</u> у игрока стоит структурная пустота** (`minecraft:structure_void`).


**Начало паркура**

Как только игрок начнет паркур, он получает метадату `ru.dfhub.parkourio.parkour.started_at`, в качестве значения которой выступает таймштамп (в миллисекундах), когда игрок был обнаружен на блоке старта. Позже, из неё будет высчитываться время, потраченное на тот или иной отрезок паркура - старт-чекпоинт, старт-финиш.

Только имея данную метадату, у игрока будут работать финиш и чекпоинты.


**Чекпоинты**

Если игрок достиг чейпоинта, то, если он уже не достиг его(про это ниже), то его метадата `...checkpoint` меняется на id этого чекпоинта. 

Если игрок достиг чекпоинта с `id = n`, то чекпоинт не будет работать, пока `metadata_checkpoint_id = n`. 
Простыми словами, если игрок достиг чекпоинта, например, с `id = 3`, то пока у него в метадате записан именно этот id, чекпоинт не будет засчитываться снова. Это сделано для того, чтобы при падении и телепортации на чекпоинт, каждый раз не выводилось новое сообщение о времени

**Завершение паркура**

Если игрок достиг финиша, то, помимо подсчета времени при помощи метадаты `...started_at`, его метадата `...checkpoint` будет выставлена на -1, чтобы при падении с уровня, он снова оказался на старте паркура, с не на последнем чекпоинте

## Команда /world
Команда /world содержит в себе 3 основных функции:
- Создание (create)
- Телепортация (tp)
- Загрузка (load)

### /world create
Команда /world create создает новый мир. При создании нового мира лучше убедиться, что на сервере нет других игроков, так как создание мира - **ресурсозатратная операция.**

Вот минимальный вид этой команды: `/world create <name> <type>`.

Также у данной команды есть необязательные флаги:
- --no-generate-structures (отключение генерации структур)
- --tp (телепортация в мир после создания)
- --empty (создание пустого мира, без блоков)

Рекомендуется вместо флага `--tp` использовать команду `/world tp`, так как моментальная телепортация может вызвать проблемы с рендером чанков, что приведёт к вылету с сервера.

### /world tp
Просто команда, которая телепортирует на нулевые координаты в тот или ной **загруженный** мир. 

Имеет единственный вид: `/world tp <name>`

### /world load
Команда для загрузки мира на сервер

По-стандарту сервера загружают только 3 стандартных мира - world, world_nether и world_the_end. Иные миры загружаются самостоятельно при помощи плагина. 

Данная команда позволяет загрузить мир на сервер, чтобы им можно было пользоваться, если он не зарегистрировался автоматически.

Имеет единственный вид: `/world load <name>`

Если указать мир, который ранее не был создан, то будет создан новый мир, без укзания настроек.

## Команда /metadata-util
Множество фишек плагина работает через метадаты. В игре нет встроеной команды для работы с ними, но есть кастомная `/metadata-util`

Команда /metadata-util содержит в себе 3 основные функции:
- Получение метадаты (get)
- Установка метадаты (set)
- Удаление метадаты (remove)

Вот примеры использования команды:
`/metadata-util get <name>`

`/metadata-util set <name> <string-value>`

`/metadata-util remove <name>`

Вторая команда устанавливает значение в формате String, что может не поддерживаться некоторыми частами плагина, потому его стоит использовать осторожно. Устанавливает метадату он только на игрока, заменяя предыдущее с таким названием, если оно было.