package edu.umbc.bft.net.nodes.impl;

import java.util.Map;
import java.util.Random;

import edu.umbc.bft.net.bean.FaultySwitchManager;
import edu.umbc.bft.net.conn.Interface;
import edu.umbc.bft.net.nodes.bzt.ByzantineSwitch;
import edu.umbc.bft.net.nodes.bzt.FaultyBehavior;
import edu.umbc.bft.net.packet.Packet;
import edu.umbc.bft.net.packet.payload.Identification;
import edu.umbc.bft.secure.RSAPriv;
import edu.umbc.bft.secure.RSAPub;
import edu.umbc.bft.util.LogValues;
import edu.umbc.bft.util.Logger;

public class FaultySwitch extends GeneralSwitch	implements ByzantineSwitch	{
	
	private FaultyBehavior behavior;
	
	public FaultySwitch(Map<String, RSAPub> tkl)	{
		super(tkl);
		FaultySwitchManager fsm = new FaultySwitchManager(super.manager, (ByzantineSwitch)this);
		this.behavior = fsm.getBehavior();
		super.manager = fsm;
	}//End of Constructor
	
	public FaultySwitch(Map<String, RSAPub> tkl, int id)	{
		this(tkl);
		this.setName("FS"+ id);
		this.behavior.setName(super.getName());
	}//End of Constructor
	
	
	@Override
	public Random getRandomBehavior()	{
		return this.behavior.getRandBehavior();
	}
	@Override
	public Map<String, RSAPriv> getFaultyKeyList()	{
		return this.behavior.getFaultyKeyList();
	}
	
	@Override
	protected final void execute(Interface i, Packet p) {
		
		Logger.sysLog(LogValues.info, this.getClass().getName(), this.subLog() +" Received: "+ p.dscp() );
		
		if( this.doFaultyBehaviour() || this.behavior.isFaulty() )	{
			this.inject(i, p);
			this.behavior.makeFaulty();
		}else
			super.execute(i, p);
			
	}//end of method
	
	
	@Override
	public void inject(Interface i, Packet p) {

		/** ------------------------------ FAULTY SWITCH ----------------------------- **/	
		switch( p.getPayload().getClass().getSimpleName() )		{

			case "PublicKeyList":
				/** Cannot do anything - as signed and encrypted by T.N. */
				super.execute(i, p);
				super.stopTimers();
				break;

			case "Identification":
				
				Identification idp = (Identification)p.getPayload();
				super.execute(i, p);
				
				if( this.behavior.checkIdSpoof() )		{
					
					Logger.sysLog(LogValues.imp, this.getClass().getName(), this.subLog() +" Faulty Behavior " );
					Identification fakeIdp = this.behavior.getIdSpoof(idp.getName());
					this.behavior.getIdSpoof(fakeIdp.getName());
					this.behavior.setIdPacket(p.getPayload());
					this.manager.checkAndFloodIdentificationMessage(p, super.getInterfaceManager());

				}else if( this.behavior.isSpoofed()==false )	{
					Logger.sysLog(LogValues.info, this.getClass().getName(), this.subLog() +" ID("+ p.getSource() +") SET " );
					this.behavior.setIdPacket(p.getPayload());
				}else	{
					Logger.sysLog(LogValues.info, this.getClass().getName(), this.subLog() +" ID("+ p.getSource() +") sent " );
					Packet pid = this.manager.createIdentificationMessage();
					super.replyFrom(i, pid);
				}
				
				break;
				
			case "LinkState":
				/** Link cost changes handled in manager and broadcast */
				super.execute(i, p);
				break;
				
			case "Datagram":
				break;
				
			case "PKLAck":
				break;
				
			case "DataAck":
				break;
				
			default:
				Logger.sysLog(LogValues.info, this.getClass().getName(), this.subLog() +" UnIdentified Packet Received " );
				break;
		
		}//end of switch
		
	}//End Of Method
	
	
	@Override
	public void forward(Interface src, Packet p) {
		// TODO Add faulty behavior
	}
	
	@Override
	public void init() {
		super.init();
		// TODO add faulty behavior
	}
	
}
