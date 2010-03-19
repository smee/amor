Export the needed AMOR backend bundles into this directory via
- right mouse click on any open project
- select "Export"
- select "Deployable plug-ins and fragments"
- select the following AMOR projects: org.infa.amor.backend.api,org.infa.amor.backend,org.infa.amor.backend.impl,org.infa.amor.backend.neo,org.infa.amor.backend.neoblobstorage
- enter the path to this directory (amor_jars) as location
- remove the version parts of the exported jar files
- run ../runServer.bat
- PROFIT!