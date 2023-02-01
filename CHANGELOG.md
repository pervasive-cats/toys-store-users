# [1.0.0-beta.8](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.7...v1.0.0-beta.8) (2023-01-30)


### Bug Fixes

* fix equals and hashcode methods in entities implementation ([6de5471](https://github.com/pervasive-cats/toys-store-users/commit/6de54711d8f997bab7f4b6c3a735c8837b94bd82))

# [1.0.0-beta.7](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.6...v1.0.0-beta.7) (2023-01-29)


### Bug Fixes

* fix deregister method name in Repository ([ae498bc](https://github.com/pervasive-cats/toys-store-users/commit/ae498bca68a20b1da6ee2c88d19dc05ff48ec779))
* fix PasswordAlgorithm service so as its check method returns a Validated and not a Boolean ([46cfe11](https://github.com/pervasive-cats/toys-store-users/commit/46cfe117d09a76709501bc7e88854e84853e7a7d))
* remove test configuration file from excluded files from git ([1f2bfdd](https://github.com/pervasive-cats/toys-store-users/commit/1f2bfddac92178b155f296f43ea4598eba3accad))
* update customer Repository constructor for accepting directly a Config instance ([6d05741](https://github.com/pervasive-cats/toys-store-users/commit/6d0574159e99f5419ae1920ea3f44eae0f63361a))


### Features

* add actor for communicating with message broker ([f3faf19](https://github.com/pervasive-cats/toys-store-users/commit/f3faf19733b131b9197290436310740a8dd3af79))
* add customer routes ([ef8d37a](https://github.com/pervasive-cats/toys-store-users/commit/ef8d37aafa4b9ce77e8d544b810cadf422a795f5))
* add CustomerServerActor for handling customer requests ([e38f2b6](https://github.com/pervasive-cats/toys-store-users/commit/e38f2b6f90a944eda9b0185cec4792751fdf3601))
* add root actor and main class ([eaefe30](https://github.com/pervasive-cats/toys-store-users/commit/eaefe30052389fde4c53de7829412f342671fd67))

# [1.0.0-beta.6](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.5...v1.0.0-beta.6) (2023-01-23)


### Bug Fixes

* Add scalafmt and scalafixAll ([337f19e](https://github.com/pervasive-cats/toys-store-users/commit/337f19ef8cf7b6f252f581919cb418792c366b8e))
* Administration creation test ([f28efb4](https://github.com/pervasive-cats/toys-store-users/commit/f28efb42d6dbba1906abfe98ffeed66d550da128))
* move used RepositoryError methods in to Repository ([e4a4253](https://github.com/pervasive-cats/toys-store-users/commit/e4a425376e8cccf3caa39c4bd0a9e7403de090b5))
* removed .properties from gitignore ([6406ef1](https://github.com/pervasive-cats/toys-store-users/commit/6406ef11df378a5c9deec381e3739c7a64c7df19))
* removed INSERT administration in user.sql in root level ([454f53f](https://github.com/pervasive-cats/toys-store-users/commit/454f53f0e45f12ea6114a6387247db964bb3ef7a))
* resolve merge conflicts ([dc537c1](https://github.com/pervasive-cats/toys-store-users/commit/dc537c1e24e8c6eea0b36e75c28eee31271c288c))
* resolve version conflict ([3158b16](https://github.com/pervasive-cats/toys-store-users/commit/3158b1694f4575fded908f2844d30065df94574f))
* update Administration RepositoryTest methods ([6f21028](https://github.com/pervasive-cats/toys-store-users/commit/6f21028ea001abfbf31d4526493d5c7b125a0dfc))


### Features

* add Administration ([ea01879](https://github.com/pervasive-cats/toys-store-users/commit/ea0187912225491dd44856de834cb75d049f9ae3))
* add Administration ([975c5e5](https://github.com/pervasive-cats/toys-store-users/commit/975c5e534220f9e71b6f4f37f9df9280cc51f9c4))
* add administration repositor ([575a83f](https://github.com/pervasive-cats/toys-store-users/commit/575a83fde8658ccd6ecf190b597e3afda8898cd1))
* add findByUsername test ([71c95cb](https://github.com/pervasive-cats/toys-store-users/commit/71c95cb57a700ceb0fed5242249c93172aa310ed))
* add findPassword test ([90ac6f6](https://github.com/pervasive-cats/toys-store-users/commit/90ac6f6cb5c21c175186b640d2da914e303c0199))
* add PostgreSQL container for testing ([dbd5248](https://github.com/pervasive-cats/toys-store-users/commit/dbd52482fedad4cc42147cd4149bb10ef772c9de))

# [1.0.0-beta.5](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.4...v1.0.0-beta.5) (2023-01-22)


### Bug Fixes

* add application.properties for testing ([2870a78](https://github.com/pervasive-cats/toys-store-users/commit/2870a789f368536f9369248e27467224598ab534))
* scalafix formatting ([05c0912](https://github.com/pervasive-cats/toys-store-users/commit/05c09127917d54bde1c1f36a7517b33562f22c55))
* scalafmt formatting ([7cca693](https://github.com/pervasive-cats/toys-store-users/commit/7cca693bd9df6eb1d842f257ce89566847bc5364))
* store manager repository test ([101944f](https://github.com/pervasive-cats/toys-store-users/commit/101944f2c780f67fe5753dcffa8e64c3eec06a89))
* update store manager ops with generics ([6634b97](https://github.com/pervasive-cats/toys-store-users/commit/6634b973c279d592ca2b75b5b5039b0703eb7930))


### Features

* add === ([f078e78](https://github.com/pervasive-cats/toys-store-users/commit/f078e78e42bdf40cd84868797b9799b609b41bfd))
* add findByUsername to store manager repo ([8f2a143](https://github.com/pervasive-cats/toys-store-users/commit/8f2a143d87cd8f65b99b7c919dab8d61e24a4f6a))
* add registration method to store manager repo ([25c1a5e](https://github.com/pervasive-cats/toys-store-users/commit/25c1a5eec40f815ae8714a5b744b5abcb40db3c3))
* add store manager operations ([875e468](https://github.com/pervasive-cats/toys-store-users/commit/875e4682414321e25471a0c7b52aecd963b74d29))
* add store manager repository trait ([769e69d](https://github.com/pervasive-cats/toys-store-users/commit/769e69ded963f8924e4e6b2b74611ff60f39f8b8))
* add Store value object ([ad94dc7](https://github.com/pervasive-cats/toys-store-users/commit/ad94dc714d20d6aa6833380b0b5ab2314b3cec25))
* add StoreManager entity ([7c63930](https://github.com/pervasive-cats/toys-store-users/commit/7c63930312b1eea1d9d0c8c9145843efc0f372a2))
* add unregister method ([99c8b90](https://github.com/pervasive-cats/toys-store-users/commit/99c8b90e9401c6a830481323965ffa3783c03573))
* add updatePassword and findPassword to store manager repository, refactor tests ([e9c3218](https://github.com/pervasive-cats/toys-store-users/commit/e9c3218cad2ff2ca8dfafa485242382af5d632a1))
* add updateStore method ([71dda38](https://github.com/pervasive-cats/toys-store-users/commit/71dda3876e3a24417da3a1acf7dd7605782f1f52))

# [1.0.0-beta.4](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.3...v1.0.0-beta.4) (2023-01-19)


### Bug Fixes

* add module for equals operator without cooperative equality ([44f7d64](https://github.com/pervasive-cats/toys-store-users/commit/44f7d645386d842620ab18e8b5166363fd1d9cca))
* add refined type for encrypted password ([2585e4e](https://github.com/pervasive-cats/toys-store-users/commit/2585e4eb220979bd30661a9be56e9901a52d5c6b))
* fix error in architecture translation ([6ee979f](https://github.com/pervasive-cats/toys-store-users/commit/6ee979fcfdbe5a37ad4ae9e9fca6ea9f264f4553))
* fix for scalafmt and scalafix ([2a9557a](https://github.com/pervasive-cats/toys-store-users/commit/2a9557a5b1da45701011fcf8fdcd18b428cc255d))
* fix formatting for scalafmt and scalafix ([a41f5ac](https://github.com/pervasive-cats/toys-store-users/commit/a41f5acfa90c53d9cdbe457e73362f5ea2314c68))
* include changes from encrypted-password branch onto this ([e1a6915](https://github.com/pervasive-cats/toys-store-users/commit/e1a6915708a3f09feb3f95152971f49babb2e06a))


### Features

* add Customer entity ([2e0d488](https://github.com/pervasive-cats/toys-store-users/commit/2e0d488d88734ee051fdf78bbf06533c73f2ec0d))
* add customer repository ([4bf6a99](https://github.com/pervasive-cats/toys-store-users/commit/4bf6a99b0b586a6581ec202208e54086b5c384c0))
* add CustomerUnregistered domain event ([d9a330f](https://github.com/pervasive-cats/toys-store-users/commit/d9a330f95c0aa3ed66cd6e164af64cf9a3cfe923))
* add Email value object ([2e5f472](https://github.com/pervasive-cats/toys-store-users/commit/2e5f472953517b1b5afa78e8978e4b1f8f5afdef))
* add NameComponent value object ([9e75238](https://github.com/pervasive-cats/toys-store-users/commit/9e75238b71db1b18353576a1a7817f5790e0a8d2))

# [1.0.0-beta.3](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.2...v1.0.0-beta.3) (2023-01-18)


### Bug Fixes

* add refined type for encrypted password ([c30f675](https://github.com/pervasive-cats/toys-store-users/commit/c30f6753e9601217f4cbcc4d15248c074a7d4f52))
* fix formatting for scalafmt and scalafix ([96901f6](https://github.com/pervasive-cats/toys-store-users/commit/96901f6208e9d219337e9da1736347ebea9d3cb3))

# [1.0.0-beta.2](https://github.com/pervasive-cats/toys-store-users/compare/v1.0.0-beta.1...v1.0.0-beta.2) (2022-12-18)


### Features

* add schemas for administration, store managers, customers ([d22953a](https://github.com/pervasive-cats/toys-store-users/commit/d22953ad683bc75e7d61dc9a00e42a2db5010742))

# 1.0.0-beta.1 (2022-12-17)


### Bug Fixes

* update username building rule, add more tests ([1e3142c](https://github.com/pervasive-cats/toys-store-users/commit/1e3142c1ddc3caab99b0fbd4421bcb80915858af))


### Features

* add EncryptedPassword value object and PasswordAlgorithm service ([0453ff2](https://github.com/pervasive-cats/toys-store-users/commit/0453ff2ec49b5b266dfd7fc569730f31cc2720bd))
* add PlainPassword value object ([9b97c5a](https://github.com/pervasive-cats/toys-store-users/commit/9b97c5aa367ab2189a4bbf8610caeba2d6199cb4))
* add user datatype and typeclass with its operations ([ca87acb](https://github.com/pervasive-cats/toys-store-users/commit/ca87acb9e7009b5f95f19b292b8aadb48c1af922))
* add Username value object ([0cb93cb](https://github.com/pervasive-cats/toys-store-users/commit/0cb93cb80fca5d2fc1da65cfc12fdabab4b4e5fd))
* make PasswordAlgorithm a given, add Repository trait, update tests for default password algorithm and mocked user ([89f0eae](https://github.com/pervasive-cats/toys-store-users/commit/89f0eae765b38ed55aabafc7003f4e70950f6478))
