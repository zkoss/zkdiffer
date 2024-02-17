# zkdiffer - A ZK diffing algorithm for ZK components

This library is inspired from [diffDom](https://github.com/fiduswriter/diffDOM) and ported in Java for ZK framework.

## License

This project is licensed under the LGPL v. 3. For details see LICENSE.txt.


## How to release FL
```bash
./gradlew clean build release -PFL=true
```
and then the released bundle file will be in `zkdiffer/build/dist` folder.

## How to release official
```bash
./gradlew clean build release
```
and then the released bundle file will be in `zkdiffer/build/dist` folder.