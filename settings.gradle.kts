rootProject.name = "data-quality-rule-engine"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

include(":engine")
include(":examples:rules-catalog-java")
include(":examples:http-api")
