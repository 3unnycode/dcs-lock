package com.xiaoseller.dcs.lock;

import java.io.Serializable;
import java.util.UUID;

public class Secret implements Serializable {
	private static final long serialVersionUID = 3760919555107775987L;
	// 唯一标识，相当于对当前锁起了个名字
	private UUID uuid;
	// 过期时间
	private long expireTime;

	public Secret() {
		super();
	}

	/**
	 * 
	 * @param uuid
	 * @param expirationTime 过期时间 单位毫秒，例如，3000,3秒过期
	 */
	public Secret(UUID uuid, long expireTime) {
		super();
		this.uuid = uuid;
		this.expireTime = expireTime;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public String toString() {
		return uuid.toString() + ":" + expireTime;
	}
	
	public boolean isExpired() {
		return getExpireTime() < System.currentTimeMillis();
	}

	public boolean isExpiredOrMine(UUID otherUUID) {
		return this.isExpired() || this.getUuid().equals(otherUUID);
	}
	
	public static Secret format(String secretString) {
		if (secretString == null || secretString.trim().equals("")) {
			return null;
		}
		try {
			String[] parts = secretString.split(":");
			UUID theUUID = UUID.fromString(parts[0]);
			long theTime = Long.parseLong(parts[1]);
			return new Secret(theUUID, theTime);
		} catch (Exception e) {
			return null;
		}
	}
}
