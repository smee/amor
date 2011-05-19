Command line based client using Equinox OSGi console features. 
Start the client via the "Amor Client".launch eclipse configuration
and type 'amorhelp' to list all available commands.

Examples:
# change to our test models directory
lcd ../../../org.infai.backend.tests/bin
lls
newbranch trunk
starttransaction trunk
add testmodels/multi/A.ecore
# we get informed about a missing model dependency
add testmodels/multi/B.ecore
committransaction "added dependant models" "max mustermann"

#create a branch starting at the last revision
newbranch subbranch trunk 1
starttransaction subbranch
add testmodels/filesystem.ecore
add testmodels/simplefilesystem.xmi
committransaction "added simple filesystem model+instance" "max mustermann"

#show detailed revision infos
cd trunk/1
dir -l
cd ../..

# checkin a patch for an checked in model
starttransaction subbranch
addpatch testmodels/simplefilesystem.xmi testmodels/fs/v1-v2.epatch
committransaction "added one newfile.txt" "max mustermann"

# checkout patched model to /temp
cd subbranch
cd 3
lcd ../../temp
checkout testmodels/simplefilesystem.xmi

starttransaction trunk
add testmodels/cdo.ecore
committransaction "added CDO ecore with nasty ecore datatypes" "max mustermann"
cd ../../trunk/4
checkout testmodels/cdo.ecore

######################### OR use this scenario ############################
lcd ../../../org.infai.backend.tests/bin
newbranch trunk
starttransaction trunk
add testmodels/bflow/bflow.ecore
add testmodels/bflow/oepc.ecore
committransaction "added bflow meta model" "max mustermann"

starttransaction trunk
add testmodels/bflow/sample_ext.xmi
add testmodels/bflow/external.xmi
committransaction "added bflow instance model" "max mustermann"

starttransaction trunk
addpatch testmodels/bflow/sample_ext.xmi testmodels/bflow/sample_v1_v2.epatch
committransaction "simple change to instance" "max mustermann"

starttransaction trunk
delete testmodels/bflow/sample_ext.xmi
committransaction "deleted instance" "max mustermann"
