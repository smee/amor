File system based org.infai.amor.backend.storage.Storage implementation. Creates a subdirectory per branch with revision sub directories. 
Each model gets persisted via XMI. Changed models are stored as complete models by applying the epatch describing
the changes to the predecessor model. 