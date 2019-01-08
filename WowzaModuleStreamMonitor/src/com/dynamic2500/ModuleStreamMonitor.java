package com.dynamic2500;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.wowza.wms.amf.AMFPacket;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.application.WMSProperties;
import com.wowza.wms.module.ModuleBase;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.stream.IMediaStreamActionNotify2;
import com.wowza.wms.stream.MediaStreamMap;

public class ModuleStreamMonitor extends ModuleBase
{

	/**
	 * monitorInterval: interval between check signal
	 * monitorPath: Path to save monitor result
	 */
	int monitorInterval = 0;
	String monitorPath = "";
	public void onAppStart(IApplicationInstance appInstance) {
		monitorInterval = appInstance.getProperties().getPropertyInt("mornitorInterval", 30000);
		monitorPath = appInstance.getProperties().getPropertyStr("monitorPath","livemonitor");
	}
	
	@SuppressWarnings("unchecked")
	public void onStreamCreate(IMediaStream stream)
	{
		getLogger().info("onStreamCreate by: " + stream.getClientId());
		IMediaStreamActionNotify2 actionNotify = new StreamListener();

		WMSProperties props = stream.getProperties();
		synchronized(props)
		{
			props.put("streamActionNotifier", actionNotify);
		}
		stream.addClientListener(actionNotify);
	}

	public void onStreamDestroy(IMediaStream stream)
	{
		getLogger().info("onStreamDestroy by: " + stream.getClientId());

		IMediaStreamActionNotify2 actionNotify = null;
		WMSProperties props = stream.getProperties();
		synchronized(props)
		{
			actionNotify = (IMediaStreamActionNotify2)stream.getProperties().get("streamActionNotifier");
		}
		if (actionNotify != null)
		{
			stream.removeClientListener(actionNotify);
			getLogger().info("removeClientListener: " + stream.getSrc());
		}
	}

	class StreamListener implements IMediaStreamActionNotify2
	{
		public void onPlay(IMediaStream stream, String streamName, double playStart, double playLen, int playReset)
		{
			streamName = stream.getName();
			getLogger().info("Stream Name: " + streamName);
		}

		public void onMetaData(IMediaStream stream, AMFPacket metaDataPacket)
		{
			getLogger().info("onMetaData By: " + stream.getClientId());
		}

		public void onPauseRaw(IMediaStream stream, boolean isPause, double location)
		{
			getLogger().info("onPauseRaw By: " + stream.getClientId());
		}

		public void onSeek(IMediaStream stream, double location)
		{
			getLogger().info("onSeek");
		}

		public void onStop(IMediaStream stream)
		{
			getLogger().info("onStop By: " + stream.getClientId());
		}

		public void onUnPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			getLogger().info("onUNPublish");
			StreamMonitor Monitor = (StreamMonitor)stream.getClient().getAppInstance().getProperties().getProperty(streamName);
			if (Monitor != null)
				Monitor.stop();
		}

		public void onPublish(IMediaStream stream, String streamName, boolean isRecord, boolean isAppend)
		{
			StreamMonitor Monitor = new StreamMonitor();
			try
			{
				Monitor.appInstance = stream.getClient().getAppInstance();
				Monitor.vHostPath = stream.getClient().getVHost().getHomePath();
				Monitor.streamName = streamName;
				Monitor.AppName = stream.getClient().getApplication().getName();
				Monitor.start();
				stream.getClient().getAppInstance().getProperties().setProperty(streamName, Monitor);
			}
			catch (Exception ex)
			{

			}
		}

		public void onPause(IMediaStream stream, boolean isPause, double location)
		{
			getLogger().info("onPause");
		}
	}

	private class StreamMonitor
	{
		public Timer mTimer;
		public TimerTask mTask;
		public String streamName;
		public String vHostPath;
		public String AppName;
		public IApplicationInstance appInstance;
		long streamLastSeq;
		long currSeq = 0;
		String msg;

		public StreamMonitor()
		{
			mTask = new TimerTask()
			{
				public void run()
				{
					getLogger().info("Run StreamMonitor channel: "+AppName+"/"+streamName);
					MediaStreamMap mediamap = appInstance.getStreams();
					IMediaStream stream = mediamap.getStream(streamName);
					List<AMFPacket> packets = stream.getPlayPackets();
					int status=0,bitrate=0;
					if (packets.size() == 0)
					{
						msg = "Fail";
					}
					else
					{
						AMFPacket packet = (AMFPacket)packets.get(packets.size() - 1);
						currSeq = packet.getSeq();
						if (currSeq != streamLastSeq)
						{
							streamLastSeq = currSeq;
							msg = "OK";
							status=1;
							bitrate=stream.getPublishBitrateVideo(1)+stream.getPublishBitrateAudio(1);
						}
						else
						{
							msg = "Stalled";
						}
					}
					
/* 					Just for testing					
  					getLogger().info("msg: " + msg);
					appInstance.broadcastMsg("streamStats", streamName, currSeq, msg, new Date());
					String param = "status="+msg+"&streamname="+streamName+"&audiosize="+stream.getAudioSize()+"&videosize="+stream.getVideoSize();
					getLogger().info("http://192.168.1.2:9999/?"+param);
					getLogger().info(getPortalAPI("192.168.1.2:9999/?"+param));
					getLogger().info("Play packet: "+stream.getPlayPackets().toString());				
					getLogger().info("Video bitrate: "+stream.getPublishBitrateVideo(1));
					getLogger().info("Audio bitrate: "+stream.getPublishBitrateAudio(1));
					getLogger().info("Video Frame: "+stream.getPublishFrameCountVideo());*/
					
					writetxt(status+":"+bitrate);
				}
			};
		}
		
		/**
		 * Out data to file
		 * @param data
		 */
		private void writetxt(String data) {
			FileWriter fw;
			try {
				fw = new FileWriter(vHostPath
						+ "/"+monitorPath+"/" + AppName + "/"+streamName);
				fw.write(data);
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
		
		/**
		 * Start Monitor
		 */
		public void start()
		{
			try {
				Files.createDirectories(Paths.get(vHostPath
						+ "/"+monitorPath+"/" + AppName));
			} catch (IOException e1) {
				getLogger().info(e1);
				e1.printStackTrace();
				if (Files.isDirectory(Paths.get(vHostPath
						+ "/"+monitorPath)) == false) {
					try {
						Files.createDirectories(Paths.get(vHostPath
								+ "/"+monitorPath));
					} catch (IOException e) {
						getLogger().info(e);
						e.printStackTrace();
					}
				}
			}
			if (mTimer == null)
				mTimer = new Timer();
			mTimer.schedule(mTask, monitorInterval, monitorInterval);
			getLogger().info("Start StreamMonitor");
		}

		/**
		 * Stop Monitor
		 */
		public void stop()
		{
			if (mTimer != null)
			{
				mTimer.cancel();
				mTimer = null;
				getLogger().info("Stop StreamMonitor");
			}
		}
	}
}
