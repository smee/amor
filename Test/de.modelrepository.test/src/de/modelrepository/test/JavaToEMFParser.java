package de.modelrepository.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emftext.language.java.JavaClasspath;
import org.emftext.language.java.JavaPackage;
import org.emftext.language.java.resource.JavaSourceOrClassFileResourceFactoryImpl;
import org.emftext.language.java.resource.java.analysis.helper.JavaPostProcessor;
import org.emftext.language.primitive_types.Primitive_typesPackage;
import org.emftext.runtime.IOptions;

public class JavaToEMFParser {
	//the ResourceSet which loads the java-resources.
	private final ResourceSet rs = new ResourceSetImpl();

	public JavaToEMFParser() {
		//Initialize the ResourceSet for loading and carrying the java models and register the common file endings.
		EPackage.Registry.INSTANCE.put("http://www.emftext.org/java", JavaPackage.eINSTANCE);
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("java", new JavaSourceOrClassFileResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		rs.getLoadOptions().put(IOptions.RESOURCE_POSTPROCESSOR_PROVIDER, new JavaPostProcessor());
	}
	
	/**
	 * Method parses the given java file and returns the {@link ResourceSet} which contains the model {@link Resource}.
	 * @param javaFile the java file as an {@link File}-object.
	 * @param JarArchives a {@link Vector} containing {@link File}-objects for the jar-archives that are referenced by this class.
	 * Entries of this Vector may be single archives or folders which contain several libraries.<br>
	 * If there are no archives needed for parsing the java file then set this variable <code>null</code>. 
	 * @return The {@link ResourceSet} which contains the corresponding EMF-model for the parsed java file.<br>
	 * The ResourceSet does also contain models for referenced classes. 
	 * @throws ProxyException This exception will be thrown if there are any referenced classes which can not be resolved by this parser.<br>
	 * Normally there are missing libraries which you have to specify for parsing the file. 
	 */
	public ResourceSet parseJavaFile(File javaFile, Vector<File> JarArchives) throws ProxyException {
		parseJavaFiles(javaFile, getAllJarFilesFromSource(JarArchives));
		return rs;
	}
	
	/**
	 * Method parses all java files from the given folder and returns a {@link ResourceSet} which contains a model {@link Resource} for each file.
	 * @param inputFolder the folder which contains the files to parse.
	 * @param JarArchives a {@link Vector} containing {@link File}-objects for the jar-archives that are referenced by the classes in this folder.
	 * Entries of this Vector may be single archives or folders which contain several libraries.<br>
	 * If there are no archives needed for parsing the java file then set this variable <code>null</code>.
	 * @return The {@link ResourceSet} which contains the corresponding EMF-models for the parsed java files.<br>
	 * The ResourceSet does also contain models for referenced classes. 
	 * @throws ProxyException This exception will be thrown if there are any referenced classes which can not be resolved by this parser.<br>
	 * Normally there are missing libraries which you have to specify for parsing the file. 
	 */
	public ResourceSet parseAllJavaFiles(File inputFolder, Vector<File> JarArchives) throws ProxyException {
		parseJavaFiles(inputFolder, getAllJarFilesFromSource(JarArchives));
		return rs;
	}
	
	/**
	 * Method parses all java files from the given folder and serializes them as EMF-models (xmi-format) to the specified folder.
	 * @param inputFolder the folder which contains the files to parse.
	 * @param JarArchives a {@link Vector} containing {@link File}-objects for the jar-archives that are referenced by the classes in this folder.
	 * Entries of this Vector may be single archives or folders which contain several libraries.<br>
	 * If there are no archives needed for parsing the java file then set this variable <code>null</code>.
	 * @param outputFolder the folder which shall contain the parsed model files.
	 * @throws ProxyException This exception will be thrown if there are any referenced classes which can not be resolved by this parser.<br>
	 * Normally there are missing libraries which you have to specify for parsing the file. 
	 */
	public void parseAndSerializeAllJavaFiles(File inputFolder, Vector<File> JarArchives, File outputFolder) throws ProxyException {
		parseJavaFiles(inputFolder, getAllJarFilesFromSource(JarArchives));
		serializeMetaModel(outputFolder);
		serializeModel(inputFolder, outputFolder);
	}
	
	/**
	 * Method parses the given java file and serializes it as an EMF-model (xmi-format) to the specified folder.
	 * @param javaFile the java file as an {@link File}-object.
	 * @param JarArchives a {@link Vector} containing {@link File}-objects for the jar-archives that are referenced by this class.
	 * Entries of this Vector may be single archives or folders which contain several libraries.<br>
	 * If there are no archives needed for parsing the java file then set this variable <code>null</code>. 
	 * @param outputFolder the folder which shall contain the parsed model file.
	 * @throws ProxyException This exception will be thrown if there are any referenced classes which can not be resolved by this parser.<br>
	 * Normally there are missing libraries which you have to specify for parsing the file. 
	 */
	public void parseAndSerializeJavaFile(File javaFile, Vector<File> JarArchives, File outputFolder) throws ProxyException {
		parseJavaFiles(javaFile, getAllJarFilesFromSource(JarArchives));
		serializeMetaModel(outputFolder);
		serializeModel(javaFile.getParentFile(), outputFolder);
	}
	
	/*
	 * This method is responsible for parsing the file(s) given at the input.
	 * The input may be a single java file or a folder that contains several java files.
	 */
	private void parseJavaFiles(File input, Vector<File> JarArchives) throws ProxyException {
		try {
			//if there are archives register them at the classpath so that they will also be parsed.
			if(JarArchives != null) {
				JavaClasspath cp = JavaClasspath.get(rs);
				for(Iterator<File> i = JarArchives.iterator(); i.hasNext(); ) {
					File jarArchieve = i.next();
					if(jarArchieve.exists()) {
						cp.registerClassifierJar(URI.createFileURI(jarArchieve.getCanonicalPath()));
					}
				}
			}
			
			//loads all files into the ResourceSet and resolves the proxies (for getting valid links within the models).
			loadAllFilesInResourceSet(input, "java");
			resolveAllProxies();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * This method serializes the metamodels (primitive_types.ecore & java.ecore)
	 */
	private void serializeMetaModel(File outputFolder) {
		try {
			URI outputURI = URI.createFileURI(outputFolder.getCanonicalPath());
			
			URI ptEcoreURI = outputURI.appendSegment("primitive_types.ecore");
			Resource ptEcoreResource = rs.createResource(ptEcoreURI);
			ptEcoreResource.getContents().add(Primitive_typesPackage.eINSTANCE);
			
			URI javaEcoreURI = outputURI.appendSegment("java.ecore");
			Resource javaEcoreResource = rs.createResource(javaEcoreURI);
			javaEcoreResource.getContents().addAll(JavaPackage.eINSTANCE.getESubpackages());
			
			ptEcoreResource.save(null);
			javaEcoreResource.save(null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * This method is responsible for serializing all parsed models from the input folder to the output folder).
	 */
	private void serializeModel(File inputFolder, File outputFolder) {
		try {
			List<Resource> result = new ArrayList<Resource>();
			
			URI srcUri = URI.createFileURI(inputFolder.getCanonicalPath());
			URI outUri = URI.createFileURI(outputFolder.getCanonicalPath());
			
			for(Resource javaResource : new ArrayList<Resource>(rs.getResources())) {
				URI srcURI = javaResource.getURI();
				//TODO anders implementieren (Konstanten)
				if(srcURI.toString().endsWith("java.ecore") || srcURI.toString().endsWith("primitive_types.ecore")) continue;
				srcURI = rs.getURIConverter().normalize(srcURI);
			 	URI outFileURI = outUri.appendSegments(srcURI.deresolve(srcUri.appendSegment("")).segments()).appendFileExtension("xmi");
				Resource xmiResource = rs.createResource(outFileURI);
				xmiResource.getContents().addAll(javaResource.getContents());
				result.add(xmiResource);
			}
			
			Map<Object, Object> options = new HashMap<Object, Object>();
			options.put(XMIResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
			options.put(XMIResource.OPTION_PROCESS_DANGLING_HREF, XMIResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);
			for(Resource xmiResource : result) {
				xmiResource.save(options);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method loads all files with the given extension from the given input location.
	 * input can also be a single file.  
	 */
	private void loadAllFilesInResourceSet(File input, String extension){
		if(input.isDirectory()) {
			for(File f : input.listFiles()) {
				if(f.isFile()) {
					if(f.getName().endsWith(extension)) {
						loadResource(f);
					}
				}
				if(f.isDirectory()) {
					if(!f.getName().startsWith(".")) {
						loadAllFilesInResourceSet(f, extension);
					}
				}
			}
		}else {
			if(input.getName().endsWith(extension)) {
				loadResource(input);
			}
		}
	}
	
	/*
	 * Method loads the given file.
	 */
	private void loadResource(File file){
		try {
			loadResource(file.getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Method loads the given file.
	 */
	private void loadResource(String filePath) {
		loadResource(URI.createFileURI(filePath));
	}
	
	/*
	 * Method loads a file given by the URI.
	 */
	private void loadResource(URI uri) {
		rs.getResource(uri, true);
	}
	
	/*
	 * Resolves all proxies within the Resources of the ResourceSet.
	 */
	private void resolveAllProxies() throws ProxyException {
		boolean failure = false;
		//contains the proxies which couldn't be resolved (maybe there is a missing library)
		Vector<URI> notFoundProxies = new Vector<URI>();
		
		//iterate over all resources of the set and resolve all of theirs crossreferences
		//if one reference couldn't be resolved a ProxyException will be thrown containing all not resolved references
		for (Iterator<Notifier> i = rs.getAllContents(); i.hasNext();) {
			Notifier next = i.next();
			if (next instanceof EObject) {
				InternalEObject nextElement = (InternalEObject) next;
				for(EObject crElement : nextElement.eCrossReferences()) {
					crElement = EcoreUtil.resolve(crElement, rs);
					if (crElement.eIsProxy()) {
						notFoundProxies.add(((InternalEObject)crElement).eProxyURI());
						failure = true;
					}
				}
			}
		}
		if(failure) {
			ProxyException e = new ProxyException(notFoundProxies);
			throw e;
		}
	}
	
	/*
	 * Method searches all java libraries at the given sources.
	 * the vector can contain a mixture of single libraries and folders which contain several libraries.
	 * the returned vector contains file-objects for each library found.
	 */
	private Vector<File> getAllJarFilesFromSource(Vector<File> files) {
		if(files == null) {
			return null;
		}else {
			Vector<File> result = new Vector<File>();
			for(File f : files) {
				if(f.isFile() && f.getAbsolutePath().endsWith(".jar") && !result.contains(f)) {
					result.add(f);
				}else if(f.isDirectory()) {
					Vector<File> children = new Vector<File>();
					for (File file : f.listFiles()) {
						children.add(file);
					}
					for (File file : getAllJarFilesFromSource(children)) {
						if(!result.contains(file)) {
							result.add(file);
						}
					}
				}
			}
			return result;
		}
	}
}
