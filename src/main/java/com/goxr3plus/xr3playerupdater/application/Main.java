package main.java.com.goxr3plus.xr3playerupdater.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;

import com.google.gson.Gson;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import main.java.com.goxr3plus.xr3playerupdater.presenter.DownloadModeController;
import main.java.com.goxr3plus.xr3playerupdater.services.DownloadService;
import main.java.com.goxr3plus.xr3playerupdater.services.ExportZipService;
import main.java.com.goxr3plus.xr3playerupdater.tools.ActionTool;
import main.java.com.goxr3plus.xr3playerupdater.tools.InfoTool;
import main.java.com.goxr3plus.xr3playerupdater.tools.NotificationType;

public class Main extends Application {
	
	//================Variables================
	
	/**
	 * This is the folder where the update will take place [ obviously the
	 * parent folder of the application]
	 */
	private File updateFolder = new File(InfoTool.getBasePathForClass(Main.class));
	
	/**
	 * Download update as a ZIP Folder , this is the prefix name of the ZIP
	 * folder
	 */
	private static String foldersNamePrefix;
	
	/** Update to download */
	private static int update;
	
	/** The name of the application you want to update */
	private String applicationName;
	
	//================Listeners================
	
	//Create a change listener
	ChangeListener<? super Number> listener = (observable , oldValue , newValue) -> {
		if (newValue.intValue() == 1)
			exportUpdate();
	};
	//Create a change listener
	ChangeListener<? super Number> listener2 = (observable , oldValue , newValue) -> {
		if (newValue.intValue() == 1)
			packageUpdate();
	};
	
	//================Services================
	
	DownloadService downloadService;
	ExportZipService exportZipService;
	
	//=============================================
	
	private Stage window;
	private static DownloadModeController downloadMode = new DownloadModeController();
	
	//---------------------------------------------------------------------
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		
		//Parse Arguments -> I want one parameter -> for example [45] which is the update i want
		List<String> applicationParameters = super.getParameters().getRaw();
		if (!applicationParameters.isEmpty())
			update = Integer.valueOf(applicationParameters.get(0));
		else {
			System.out.println("No arguments given...\nApplication exit.");
			System.exit(0);
		}
		
		//We need this in order to restart the update when it fails
		System.out.println("Application Started");
		
		// --------Window---------
		window = primaryStage;
		window.setResizable(false);
		window.centerOnScreen();
		window.getIcons().add(InfoTool.getImageFromResourcesFolder("icon.png"));
		window.centerOnScreen();
		window.setOnCloseRequest(exit -> {
			
			//Check
			if (exportZipService != null && exportZipService.isRunning()) {
				ActionTool.showNotification("Mensagem", "Não pode sair enquanto estiver atualizando, podendo corromper o arquivo", Duration.seconds(5), NotificationType.WARNING);
				exit.consume();
				return;
			}
			
			//Question
			if (!ActionTool.doQuestion("Quer sair " + applicationName + "  atualizador ?", window))
				exit.consume();
			else {
				
				//Delete the ZIP Folder
				deleteZipFolder();
				
				//Exit the application
				System.exit(0);
			}
			
		});
		
		// Scene
		Scene scene = new Scene(downloadMode);
		scene.getStylesheets().add(getClass().getResource(InfoTool.STYLES + InfoTool.APPLICATIONCSS).toExternalForm());
		window.setScene(scene);
		
		//Show
		window.show();
		
		//Start
		prepareForUpdate("PrintOrtolook");
	}
	
	//-------------------------------------------------------------------------------------------------------------------------------
	
	/**
	 * Prepare for the Update
	 * 
	 * @param applicationName
	 */
	public void prepareForUpdate(String applicationName) {
		this.applicationName = applicationName;
		window.setTitle(applicationName + " Atualizador");
		
		//FoldersNamePrefix	
		foldersNamePrefix = updateFolder.getAbsolutePath() + File.separator + applicationName + " Pacote de atualizacao " + update;
		
		File folder = new File(updateFolder.getAbsolutePath());
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
		  if (listOfFiles[i].isFile()) {
		    System.out.println("File " + listOfFiles[i].getName());
		    System.out.println("cheguei aqui"+listOfFiles[i].getName().substring(14, 19));
		    if(listOfFiles[i].getName().contains("PrintOrtolook")) {		    	
		    	//System.out.println(listOfFiles[i].getName().substring(14, 19));
		    }
		  } 
		}
		
		
		//Check the Permissions
		if (checkPermissions()) {
			downloadMode.getProgressLabel().setText("Verificando permissões");
			//downloadUpdate("https://github.com/goxr3plus/XR3Player/releases/download/V3." + update + "/XR3Player.Update." + update + ".zip");
			downloadUpdate("http://www.resistenciaarmada.com/teste-impressao/PrintOrtolook-"+ update + ".zip");
			
		} else {
			
			//Update
			downloadMode.getProgressBar().setProgress(-1);
			downloadMode.getProgressLabel().setText("Please close the updater");
			
			//Show Message
			ActionTool.showNotification("Permissão negada [FATAL ERROR]",
					"Atualizador não tem permissão para este diretório:\n [ " + updateFolder.getAbsolutePath()
							+ " ]\n -> Estmaos buscando uma solução para este erro \n -> Pode baixar " + applicationName + " manualmente :) ]",
					Duration.minutes(1), NotificationType.ERROR);
		}
	}
	
	/**
	 * In order to update this application must have READ,WRITE AND CREATE
	 * permissions on the current folder
	 */
	public boolean checkPermissions() {
		
		//Check for permission to Create
		try {
			File sample = new File(updateFolder.getAbsolutePath() + File.separator + "empty123123124122354345436.txt");
			/*
			 * Create and delete a dummy file in order to check file
			 * permissions. Maybe there is a safer way for this check.
			 */
			sample.createNewFile();
			sample.delete();
		} catch (IOException e) {
			//Error message shown to user. Operation is aborted
			return false;
		}
		
		//Also check for Read and Write Permissions
		return updateFolder.canRead() && updateFolder.canWrite();
	}
	
	/** Try to download the Update */
	private void downloadUpdate(String downloadURL) {
		
		if (InfoTool.isReachableByPing("www.google.com")) {
			
			//Download it
			try {
				//Delete the ZIP Folder
				deleteZipFolder();
				
				//Create the downloadService
				downloadService = new DownloadService();
				
				//Add Bindings
				downloadMode.getProgressBar().progressProperty().bind(downloadService.progressProperty());
				downloadMode.getProgressLabel().textProperty().bind(downloadService.messageProperty());
				downloadMode.getProgressLabel().textProperty().addListener((observable , oldValue , newValue) -> {
					//Give try again option to the user
					if (newValue.toLowerCase().contains("failed"))
						downloadMode.getFailedStackPane().setVisible(true);
				});
				downloadMode.getProgressBar().progressProperty().addListener(listener);
				window.setTitle("Downloading ( " + this.applicationName + " ) Update -> " + this.update);
				
				//Start
				downloadService.startDownload(new URL(downloadURL), Paths.get(foldersNamePrefix + ".zip"));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
		} else {
			//Update
			downloadMode.getProgressBar().setProgress(-1);
			downloadMode.getProgressLabel().setText("Sem conexão com a internet...");
			
			//Delete the ZIP Folder
			deleteZipFolder();
			
			//Give try again option to the user
			downloadMode.getFailedStackPane().setVisible(true);
		}
	}
	
	/** Exports the Update ZIP Folder */
	private void exportUpdate() {
		
		//Create the ExportZipService
		exportZipService = new ExportZipService();
		
		//Remove Listeners
		downloadMode.getProgressBar().progressProperty().removeListener(listener);
		
		//Add Bindings		
		downloadMode.getProgressBar().progressProperty().bind(exportZipService.progressProperty());
		downloadMode.getProgressLabel().textProperty().bind(exportZipService.messageProperty());
		downloadMode.getProgressBar().progressProperty().addListener(listener2);
		
		//Start it
		exportZipService.exportZip(foldersNamePrefix + ".zip", updateFolder.getAbsolutePath());
		
	}
	
	/**
	 * After the exporting has been done i must delete the old update files and
	 * add the new ones
	 */
	private void packageUpdate() {
		
		//Remove Listeners
		downloadMode.getProgressBar().progressProperty().removeListener(listener2);
		
		//Bindings
		downloadMode.getProgressBar().progressProperty().unbind();
		downloadMode.getProgressLabel().textProperty().unbind();
		
		//Packaging
		downloadMode.getProgressBar().setProgress(-1);
		downloadMode.getProgressLabel().setText("Starting " + applicationName + "...");
		
		//Delete the ZIP Folder
		deleteZipFolder();
		
		//Start XR3Player
		restartApplication(applicationName);
		
	}
	
	//---------------------------------------------------------------------------------------
	
	/** Calling this method to start the main Application which is XR3Player */
	@SuppressWarnings("unused")
	public static void restartApplication(String appName) {
		
		List<ProcessInfo> processesList = JProcesses.getProcessList();
	    
		
	    for (final ProcessInfo processInfo : processesList) {
	      
	        if(processInfo.getCommand().contains("PrintOrtolook")) {
	        	
	        	System.out.println("achou!");
	        	
	        	
	        	 try {
	        		 boolean success = JProcesses.killProcess(Integer.parseInt(processInfo.getPid())).isSuccess();
	        		 if(success)
	        			 System.out.println("Finalizou!");
	     		} catch (Exception e) {
	     			// TODO Auto-generated catch block
	     			e.printStackTrace();
	     			e.getMessage();
	     		}
	        }
	        
	       
	    }
	    
	  
		
		 //Restart XR3Player
		new Thread(() -> {
			String path = InfoTool.getBasePathForClass(Main.class);
			String[] applicationPath = { new File(path + appName + ".jar").getAbsolutePath() };
			
			//Show message that application is restarting
			Platform.runLater(() -> ActionTool.showNotification("Starting " + appName,
					"Application Path:[ " + applicationPath[0] + " ]\n\tIf this takes more than [20] seconds either the computer is slow or it has failed....", Duration.seconds(25),
					NotificationType.INFORMATION));
			
			try {
				
				//Delete the ZIP Folder
				deleteZipFolder();
				
				//------------Wait until Application is created
//				File applicationFile = new File(applicationPath[0]);
//				while (!applicationFile.exists()) {
//					Thread.sleep(50);
//					System.out.println("Waiting " + appName + " Jar to be created...");
//				}
//				
//				System.out.println(appName + " Path is : " + applicationPath[0]);
//				
				//Create a process builder
				ProcessBuilder builder = new ProcessBuilder("java", "-jar", applicationPath[0]);
				builder.redirectErrorStream(true);
				Process process = builder.start();
				//BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				
//				// Wait n seconds
//				PauseTransition pause = new PauseTransition(Duration.seconds(10));
//				pause.setOnFinished(f -> Platform.runLater(() -> ActionTool.showNotification("Starting " + appName + " failed",
//						"\nApplication Path: [ " + applicationPath[0] + " ]\n\tTry to do it manually...", Duration.seconds(10), NotificationType.ERROR)));
//				pause.play();
//				
//				// Continuously Read Output to check if the main application started
//				String line;
//				while (process.isAlive())
//					while ( ( line = bufferedReader.readLine() ) != null) {
//						if (line.isEmpty())
//							break;
//						//This line is being printed when XR3Player Starts 
//						//So the AutoUpdater knows that it must exit
//						else if (line.contains("XR3Player ready to rock!"))
//							System.exit(0);
//					}
				System.exit(0);
				
			} catch (IOException ex) {
				Logger.getLogger(Main.class.getName()).log(Level.INFO, null, ex);
				
				// Show failed message
				Platform.runLater(() -> Platform.runLater(() -> ActionTool.showNotification("Starting " + appName + " failed",
						"\nApplication Path: [ " + applicationPath[0] + " ]\n\tTry to do it manually...", Duration.seconds(10), NotificationType.ERROR)));
				
			}
		}, "Start Application Thread").start();
	}
	
	/**
	 * Delete the ZIP folder from the update
	 * 
	 * @return True if deleted , false if not
	 */
	public static boolean deleteZipFolder() {
		return new File(foldersNamePrefix + ".zip").delete();
	}
	
	public String pegaVersao() throws Exception {
		String json = lerJson("https://testerjapp.000webhostapp.com/api/v1/versao");

	    Gson gson = new Gson();        
	    Versao v = gson.fromJson(json, Versao.class);

	    return v.versao;
	}
	
	private static String lerJson(String urlString) throws Exception {
	    BufferedReader reader = null;
	    try {
	        URL url = new URL(urlString);
	        reader = new BufferedReader(new InputStreamReader(url.openStream()));
	        StringBuffer buffer = new StringBuffer();
	        int read;
	        char[] chars = new char[1024];
	        while ((read = reader.read(chars)) != -1)
	            buffer.append(chars, 0, read); 

	        return buffer.toString();
	    } finally {
	        if (reader != null)
	            reader.close();
	    }
	}
	
	static class Versao {
		String versao;
	  
	}
	
	public static void main(String[] args) {
		
		launch(args);
	}
	
}
