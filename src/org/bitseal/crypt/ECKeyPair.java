/**
 * 
 * Originally copyright Google Inc, as below. Original code released under the Apache License version 2.0.
 * 
 * Modified for use in the Android Bitmessage client "Bitseal".  
 * 
 * 
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.bitseal.crypt;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.asn1.sec.SECNamedCurves;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;

/**
 * Represents an elliptic curve keypair.
 * 
 * @author The bitcoinj developers, modified by Jonathan Coe
 */
public class ECKeyPair 
{
    private static final ECDomainParameters ecParams;
    private static final SecureRandom secureRandom;
    
    private final BigInteger priv;
    private final byte[] pub;

    static 
    {
        // All clients must agree on the curve to use by agreement. Bitcoin and Bitmessage use curve secp256k1.
        X9ECParameters params = SECNamedCurves.getByName("secp256k1");
        ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(),  params.getH());
        secureRandom = new SecureRandom();
    }

    /** 
     * Generates an entirely new keypair. 
     * */
    public ECKeyPair() 
    {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ecParams, secureRandom);
        generator.init(keygenParams);
        AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
        ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
        ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
        priv = privParams.getD();
        pub = pubParams.getQ().getEncoded();// The public key is an encoded point on the elliptic curve. It has no meaning independent of the curve.
    }

    /**
     * Creates an ECKey given only the private key. This works because EC public keys are derivable from their
     * private keys by doing a multiply with the generator value.
     */
    public ECKeyPair(BigInteger privKey) 
    {
        this.priv = privKey;
        this.pub = publicKeyFromPrivate(privKey);
    }

    /**
     * Returns the raw public key in byte[] form.
     */
    public byte[] getPubKey() 
    {
        return pub;
    }
    
    /**
     * Returns the private key in BigInteger form.
     */
    public BigInteger getPrivKey()
    {
    	return priv;
    }
    
    /** Derive the public key by doing a point multiply of G * priv. */
    public static byte[] publicKeyFromPrivate(BigInteger privKey) 
    {
        return ecParams.getG().multiply(privKey).getEncoded();
    }
}