package org.asf.emuferal.packets.xml.handshake.version;

import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;
import org.asf.emuferal.packets.xml.VersionHandshakePackets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class ServerToClientOK implements ISmartfoxPacket {
	private XmlMapper mapper;

	public ServerToClientOK(XmlMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public ISmartfoxPacket instantiate() {
		return new ServerToClientOK(mapper);
	}

	@Override
	public boolean canParse(String content) {
		return false;
	}

	@Override
	public boolean parse(String content) {
		return false;
	}

	@Override
	public String build() {
		try {
			VersionHandshakePackets.Response.msg m = new VersionHandshakePackets.Response.msg();
			m.body.action = "apiOK";
			m.body.r = "0";
			return mapper.writeValueAsString(m);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public boolean handle() {
		return false;
	}

}
