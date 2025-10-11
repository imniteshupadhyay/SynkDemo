package com.playmotech.api.core.agora;

public interface PackableEx extends Packable {
	void unmarshal(ByteBuf in);
}
