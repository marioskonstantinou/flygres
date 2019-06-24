# flygres
#### Flyway feat. Postgres
Flyway-postgres-docker integration

### Migrations/Repeatables versioning
1. Migrations - V[Epoch timestamp]__description.sql
2. Repeatables - R__[Epoch timestamp]_view_description.sql

### Project structure
1. `db` : Contains all related schemas in flyway migration process
    1. `SCHEMA_NAME`: Schema name
        1. `migrations`: Contains migration version scripts
        1. `views`: Contains repeatable versioned scripts. During migration process, if the checksum does not match the 
        one saved in db, it is executed again

### Running tests
Execute `sbt test`

### Running from sbt shell
Execute `run --environment local --schemas public --action migrate`

**Notes:**
_Make sure there's no running docker instance of postgres (port **5433**)_

### Building and pushing a docker image
1. `sbt docker`: produces a tagged image with name `com.flygres/flygres:local-build`
1. `docker push ...`

### Command line arguments
1. `Environment` (**Required**): Selected environment for execution e.g. `--environment dev` (See details in 
Environments section) 

1. `Schemas` (**Optional**): Comma separated schemas to execute. If none are passed schemas from `check_schemas` 
are selected (see env based `application.conf` configuration) `e.g. --schemas public`
 
1. `Action` (**Optional** - _Default: migrate_): Flyway action type. `e.g. --action migrate` . Available values `migrate`, `repair` (see 
flyway documentation for more details)
 
### Running from command line or within IDE
Make sure _Command line arguments_ are set with the desired values then run `MainApp`

### Example of running docker image
```
docker run --rm --net="host" flygres/flygres:local-build --environment local --schemas public --action migrate
```

### Environments
##### local_integration_test

##### local (for local development purposes)

##### dev

##### prod

##### Required environment variables (dev, prod)
```
DB_HOST
DB_PORT
DB_NAME
DB_SSLMODE
DB_USERNAME
DB_PASSWORD
```


### TODOs
1. Display branch and commit id details
1. ...
