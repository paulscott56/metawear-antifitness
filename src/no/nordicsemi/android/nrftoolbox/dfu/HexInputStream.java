/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided.
 * This heading must NOT be removed from the file.
 ******************************************************************************/

/*
 * NORDIC SEMICONDUTOR EXAMPLE CODE AND LICENSE AGREEMENT
 *
 * You are receiving this document because you have obtained example code (�Software�) 
 * from Nordic Semiconductor ASA * (�Licensor�). The Software is protected by copyright 
 * laws and international treaties. All intellectual property rights related to the 
 * Software is the property of the Licensor. This document is a license agreement governing 
 * your rights and obligations regarding usage of the Software. Any variation to the terms 
 * of this Agreement shall only be valid if made in writing by the Licensor.
 * 
 * == Scope of license rights ==
 * 
 * You are hereby granted a limited, non-exclusive, perpetual right to use and modify the 
 * Software in order to create your own software. You are entitled to distribute the 
 * Software in original or modified form as part of your own software.
 *
 * If distributing your software in source code form, a copy of this license document shall 
 * follow with the distribution.
 *   
 * The Licensor can at any time terminate your rights under this license agreement.
 * 
 * == Restrictions on license rights ==
 * 
 * You are not allowed to distribute the Software on its own, without incorporating it into 
 * your own software.  
 * 
 * You are not allowed to remove, alter or destroy any proprietary, 
 * trademark or copyright markings or notices placed upon or contained with the Software.
 *     
 * You shall not use Licensor�s name or trademarks without Licensor�s prior consent.
 * 
 * == Disclaimer of warranties and limitation of liability ==
 * 
 * YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT USE OF THE SOFTWARE IS AT YOUR OWN RISK AND THAT THE 
 * SOFTWARE IS PROVIDED *AS IS" WITHOUT ANY WARRANTIES OR CONDITIONS WHATSOEVER. NORDIC SEMICONDUCTOR ASA 
 * DOES NOT WARRANT THAT THE FUNCTIONS OF THE SOFTWARE WILL MEET YOUR REQUIREMENTS OR THAT THE 
 * OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR FREE. YOU ASSUME RESPONSIBILITY FOR 
 * SELECTING THE SOFTWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE *USE AND THE RESULTS 
 * OBTAINED FROM THE SOFTWARE.
 * 
 * NORDIC SEMICONDUCTOR ASA DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED 
 * TO WARRANTIES RELATED TO: NON-INFRINGEMENT, LACK OF VIRUSES, ACCURACY OR COMPLETENESS OF RESPONSES 
 * OR RESULTS, IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * IN NO EVENT SHALL NORDIC SEMICONDUCTOR ASA BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL OR 
 * CONSEQUENTIAL DAMAGES OR FOR ANY DAMAGES WHATSOEVER (INCLUDING BUT NOT LIMITED TO DAMAGES FOR 
 * LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, PERSONAL INJURY, 
 * LOSS OF PRIVACY OR OTHER PECUNIARY OR OTHER LOSS WHATSOEVER) ARISING OUT OF USE OR INABILITY TO 
 * USE THE SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * REGARDLESS OF THE FORM OF ACTION, NORDIC SEMICONDUCTOR ASA AGGREGATE LIABILITY ARISING OUT OF 
 * OR RELATED TO THIS AGREEMENT SHALL NOT EXCEED THE TOTAL AMOUNT PAYABLE BY YOU UNDER THIS AGREEMENT. 
 * THE FOREGOING LIMITATIONS, EXCLUSIONS AND DISCLAIMERS SHALL APPLY TO THE MAXIMUM EXTENT ALLOWED BY 
 * APPLICABLE LAW.
 * 
 * == Dispute resolution and legal venue ==
 * 
 * Any and all disputes arising out of the rights and obligations in this license agreement shall be 
 * submitted to ordinary court proceedings. You accept the Oslo City Court as legal venue under this agreement.
 * 
 * This license agreement shall be governed by Norwegian law.
 * 
 * == Contact information ==
 * 
 * All requests regarding the Software or the API shall be directed to: 
 * Nordic Semiconductor ASA, P.O. Box 436, Sk�yen, 0213 Oslo, Norway.
 * 
 * http://www.nordicsemi.com/eng/About-us/Contact-us
 */
package no.nordicsemi.android.nrftoolbox.dfu;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HexInputStream extends FilterInputStream {
	private final int LINE_LENGTH = 16;
	private final byte[] localBuf;
	private int localPos;
	private int pos;
	private int size;
	private int available, bytesRead;

	/**
	 * Creates the HEX Input Stream. The constructor calculates the size of the
	 * BIN content which is available through {@link #sizeInBytes()}. If HEX
	 * file is invalid then the bin size is 0. The supported line types are: 0
	 * (data), 4 (extended linear address). The type 5 is available of the end
	 * of the file.
	 * 
	 * @param in
	 *            the input stream to read from
	 * @throws HexFileValidationException
	 *             if HEX file is invalid. F.e. there is no semicolon (':') on
	 *             the beginning of each line.
	 * @throws IOException
	 *             if the stream is closed or another IOException occurs.
	 */
	protected HexInputStream(final InputStream in)
			throws HexFileValidationException, IOException {
		super(new BufferedInputStream(in));
		localBuf = new byte[LINE_LENGTH];
		localPos = LINE_LENGTH; // we are at the end of the local buffer, new
								// one must be obtained
		size = localBuf.length;

		available = calculateBinSize();
	}

	private int calculateBinSize() throws IOException {
		int binSize = 0;
		final InputStream in = this.in;
		in.mark(in.available());

		int b, lineSize, type;
		try {
			b = in.read();
			while (true) {
				checkComma(b);

				lineSize = readByte(in); // reading the length of the data in
											// this line
				in.skip(4); // skipping address part
				type = readByte(in); // reading the line type
				switch (type) {
				case 0x00:
					// data type line
					binSize += lineSize;
					break;
				case 0x01:
					// end of file
					return binSize;
				case 0x02:
					// extended segment address record
				case 0x04:
					// extended linear address record
				default:
					break;
				}
				in.skip(lineSize * 2 /* 2 hex per one byte */+ 2 /* check sum */);
				// skip end of line
				while (true) {
					b = in.read();

					if (b != '\n' && b != '\r') {
						break;
					}
				}
			}
		} finally {
			in.reset();
		}
	}

	@Override
	public int available() throws IOException {
		return available - bytesRead;
	}

	/**
	 * Fills the buffer with next bytes from the stream.
	 * 
	 * @return the size of the buffer
	 * @throws IOException
	 */
	public int readPacket(byte[] buffer) throws HexFileValidationException,
			IOException {
		int i = 0;
		while (i < buffer.length) {
			if (localPos < size) {
				buffer[i++] = localBuf[localPos++];
				continue;
			}

			bytesRead += size = readLine();
			if (size == 0)
				break; // end of file reached
		}
		return i;
	}

	@Override
	public int read() throws IOException {
		throw new UnsupportedOperationException(
				"Please, use readPacket() method instead");
	}

	@Override
	public int read(byte[] buffer) throws IOException {
		return readPacket(buffer);
	}

	@Override
	public int read(byte[] buffer, int offset, int count) throws IOException {
		throw new UnsupportedOperationException(
				"Please, use readPacket() method instead");
	}

	/**
	 * Returns the total number of bytes. Call this method before reading
	 * packets.
	 * 
	 * @return total number of bytes available
	 * @throws IOException
	 */
	public int sizeInBytes() throws IOException {
		return available;
	}

	/**
	 * Returns the total number of packets with given size that are needed to
	 * get all available data
	 * 
	 * @param packetSize
	 *            the maximum packet size
	 * @return the number of packets needed to get all the content
	 * @throws IOException
	 */
	public int sizeInPackets(final int packetSize) throws IOException {
		final int sizeInBytes = sizeInBytes();

		return sizeInBytes / packetSize
				+ ((sizeInBytes % packetSize) > 0 ? 1 : 0);
	}

	/**
	 * Reads new line from the input stream. Input stream must be a HEX file.
	 * The first line is always skipped.
	 * 
	 * @return the number of data bytes in the new line. 0 if end of file.
	 * @throws IOException
	 *             if this stream is closed or another IOException occurs.
	 */
	private int readLine() throws IOException {
		// end of file reached
		if (pos == -1)
			return 0;
		final InputStream in = this.in;

		// temporary value
		int b = 0;

		int lineSize, type;
		do {
			// skip end of line
			while (true) {
				b = in.read();
				pos++;

				if (b != '\n' && b != '\r') {
					break;
				}
			}

			/*
			 * Each line starts with comma (':') Data is written in HEX, so each
			 * 2 ASCII letters give one byte. After the comma there is one byte
			 * (2 HEX signs) with line length (normally 10 -> 0x10 -> 16 bytes
			 * -> 32 HEX characters) After that there is a 4 byte of an address.
			 * This part may be skipped. There is a packet type after the
			 * address (1 byte = 2 HEX characters). 00 is the valid data. Other
			 * values can be skipped when converting to BIN file. Then goes n
			 * bytes of data followed by 1 byte (2 HEX chars) of checksum, which
			 * is also skipped in BIN file.
			 */
			checkComma(b); // checking the comma at the beginning
			lineSize = readByte(in); // reading the length of the data in this
										// line
			pos += 2;
			pos += in.skip(4); // skipping address part
			type = readByte(in); // reading the line type
			pos += 2;

			// if the line type is no longer data type (0x00), we've reached the
			// end of the file
			switch (type) {
			case 0x00:
				// data type
				break;
			case 0x01:
				// end of file
				pos = -1;
				return 0;
			case 0x02:
				// extended segment address
			case 0x04:
				// extended linear address
			default:
				pos += in.skip(lineSize * 2 /* 2 hex per one byte */+ 2 /*
																		 * check
																		 * sum
																		 */);
				break;
			}
		} while (type != 0);

		// otherwise read lineSize bytes or fill the whole buffer
		for (int i = 0; i < localBuf.length && i < lineSize; ++i) {
			b = readByte(in);
			pos += 2;
			localBuf[i] = (byte) b;
		}
		pos += in.skip(2); // skip the checksum
		localPos = 0;

		return lineSize;
	}

	@Override
	public synchronized void reset() throws IOException {
		super.reset();

		pos = 0;
		bytesRead = 0;
		localPos = 0;
	}

	private void checkComma(final int comma) throws HexFileValidationException {
		if (comma != ':')
			throw new HexFileValidationException("Not a HEX file");
	}

	private int readByte(final InputStream in) throws IOException {
		final int first = asciiToInt(in.read());
		final int second = asciiToInt(in.read());

		return first << 4 | second;
	}

	private int asciiToInt(final int ascii) {
		if (ascii >= 'A')
			return ascii - 0x37;

		if (ascii >= '0')
			return ascii - '0';
		return -1;
	}
}
