package l1j.server.server.model;

import static l1j.server.server.model.skill.L1SkillId.*;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import l1j.server.Config;
import l1j.server.GameSystem.Boss.BossAlive;
import l1j.server.GameSystem.Robot.L1RobotInstance;
import l1j.server.server.ActionCodes;
import l1j.server.server.Controller.WarTimeController;
import l1j.server.server.datatables.CharacterBalance;
import l1j.server.server.datatables.CharacterHitRate;
import l1j.server.server.datatables.CharacterReduc;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.datatables.WeaponAddDamage;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.model.gametime.L1GameTimeClock;
import l1j.server.server.model.poison.L1DamagePoison;
import l1j.server.server.model.poison.L1ParalysisPoison;
import l1j.server.server.model.poison.L1SilencePoison;
import l1j.server.server.model.skill.L1SkillId;
import l1j.server.server.serverpackets.S_AttackCritical;
import l1j.server.server.serverpackets.S_AttackMissPacket;
import l1j.server.server.serverpackets.S_AttackPacket;
import l1j.server.server.serverpackets.S_AttackPacketForNpc;
import l1j.server.server.serverpackets.S_ChatPacket;
import l1j.server.server.serverpackets.S_DoActionGFX;
import l1j.server.server.serverpackets.S_NewSkillIcon;
import l1j.server.server.serverpackets.S_OwnCharAttrDef;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.S_UseArrowSkill;
import l1j.server.server.serverpackets.S_UseAttackSkill;
import l1j.server.server.types.Point;
import l1j.server.server.utils.CalcStat;
import l1j.server.server.utils.CommonUtil;

public class L1Attack {

    private L1PcInstance _pc = null;
    private L1Character _target = null;
    private L1PcInstance _targetPc = null;
    private L1NpcInstance _npc = null;
    private L1NpcInstance _targetNpc = null;

    private final int _targetId;
    private int _targetX;
    private int _targetY;
    private int _statsBonusDamage = 0;
    //private static final Random _random = new Random(System.nanoTime());
    private static ThreadLocalRandom _random = ThreadLocalRandom.current();
    private int _hitRate = 0;
    private int _calcType;
    private static final int PC_PC = 1;
    private static final int PC_NPC = 2;
    private static final int NPC_PC = 3;
    private static final int NPC_NPC = 4;
    public boolean _isHit = false;
    public boolean _isCritical = false;
    private int _damage = 0;
    private int _drainMana = 0;

    /**
     * ゾウのストーンゴーレム
     **/

    private int _drainHp = 0;

    /**
     * ゾウのストーンゴーレム
     **/

    private int _attckGrfxId = 0;
    private int _attckActId = 0;

    // 攻撃者がプレイヤーの場合の武器情報
    private L1ItemInstance weapon = null;
    private L1ItemInstance armor = null;
    private int _weaponId = 0;
    private int _weaponType = 0;
    private int _weaponType2 = 0;
    // private int _weaponType1 = 0;
    private int _weaponAddHit = 0;
    private int _weaponAddDmg = 0;
    private int _weaponSmall = 0;
    private int _weaponLarge = 0;
    private int _weaponBless = 1;
    private int _weaponEnchant = 0;
    private int _weaponMaterial = 0;
    private int _weaponDoubleDmgChance = 0;
    private int _ignorereductionbyweapon = 0;
    private int _ignorereductionbyarmor = 0;
    private int _weaponAttrLevel = 0; // 属性レベル
    private int _attackType = 0;
    private L1ItemInstance _arrow = null;
    private L1ItemInstance _sting = null;
    private int _leverage = 10; // 1/10倍表現する。
    
    // weapon types for clarity
    private short SWORD = 4;
    private short BLUNT = 11;
    private short BOW = 20;
    private short SPEAR_CHAIN = 24;
    private short STAFF = 40;
    private short DAGGER = 46;
    private short TWOHANDSWORD = 50;
    private short EDORYU = 54;
    private short KIRI_CLAW = 58;
    private short GAUNTLET = 62;
    private short ARROW = 66;
    private short THROWING = 2922;

    public void setLeverage(int i) {
        _leverage = i;
    }

    private int getLeverage() {
        return _leverage;
    }

    public void setActId(int actId) {
        _attckActId = actId;
    }

    public void setGfxId(int gfxId) {
        _attckGrfxId = gfxId;
    }

    public int getActId() {
        return _attckActId;
    }

    public int getGfxId() {
        return _attckGrfxId;
    }

    public L1Attack(L1Character attacker, L1Character target) {
        if (attacker instanceof L1PcInstance) {
            _pc = (L1PcInstance) attacker;
            if (target instanceof L1PcInstance) {
                _targetPc = (L1PcInstance) target;
                _calcType = PC_PC;
            } else if (target instanceof L1NpcInstance) {
                _targetNpc = (L1NpcInstance) target;
                _calcType = PC_NPC;
            }
            // Get weapon information
            if (_pc.hasSkillEffect(L1SkillId.SLAYER) && _pc.getSecondWeapon() != null && _pc.getSlayerSwich() == 1) {
                weapon = _pc.getSecondWeapon();
            } else {
            	weapon = _pc.getWeaponSwap();
            }

            if (weapon != null) {
                _weaponId = weapon.getItem().getItemId();
                _weaponType = weapon.getItem().getType1();
                _weaponType2 = weapon.getItem().getType(); // change
                _weaponAddHit = weapon.getItem().getHitModifier() + weapon.getHitByMagic();
                _weaponAddDmg = weapon.getItem().getDmgModifier() + weapon.getDmgByMagic();
                _weaponSmall = weapon.getItem().getDmgSmall();
                _weaponLarge = weapon.getItem().getDmgLarge();
                _weaponBless = weapon.getItem().getBless();
                if (_weaponType == 0) {
                    _weaponEnchant = 0;
                }
                if (_weaponType != BOW && _weaponType != GAUNTLET) {
                    _weaponEnchant = weapon.getEnchantLevel() - weapon.get_durability(); // Damage minus
                } else {
                    _weaponEnchant = weapon.getEnchantLevel();
                }
                _weaponMaterial = weapon.getItem().getMaterial();
                
                if (_weaponType == BOW) { // bow
                    _arrow = _pc.getInventory().getArrow();
                    if (_arrow != null) {
                        _weaponBless = _arrow.getItem().getBless();
                        _weaponMaterial = _arrow.getItem().getMaterial();
                    }
                }
                if (_weaponType == GAUNTLET) { // Get sting
                    _sting = _pc.getInventory().getSting();
                    if (_sting != null) {
                        _weaponBless = _sting.getItem().getBless();
                        _weaponMaterial = _sting.getItem().getMaterial();
                    }
                }
                _weaponDoubleDmgChance = weapon.getItem().getDoubleDmgChance();
                _weaponAttrLevel = weapon.getAttrEnchantLevel();
            }
            
            // bonus damage from stats
            if (_weaponType == BOW || _weaponType == GAUNTLET) { // range
                //_statsBonusDamage = CalcStat.calcBaseDamageFromDex(_pc.getAbility().getTotalDex()) + CalcStat.calcDEXBonusDamage(_pc.getAbility().getTotalDex());
                _statsBonusDamage = CalcStat.calcPureDEXBonusDamage(_pc.getAbility().getBaseDex());
                System.out.println("ranged pure stat bonus: " + _statsBonusDamage);
            } else if (_weaponType2 == 17) { // for kiringku
                _statsBonusDamage = CalcStat.calcINTMagicDamage(_pc.getAbility().getTotalInt()) 
                		+ CalcStat.calcINTMagicBonus(0, _pc.getAbility().getTotalInt())
                		+ CalcStat.calcPureIntBonus(_pc.getAbility().getBaseInt());
                //System.out.println("weaponType kiringku bonus: " + _statsBonusDamage);
            } else { // all others
                //_statsBonusDamage = CalcStat.calcBaseDamageFromSTR(_pc.getAbility().getTotalStr()) + CalcStat.calcSTRBonusDamage(_pc.getAbility().getBaseStr());
                _statsBonusDamage = CalcStat.calcSTRBonusDamage(_pc.getAbility().getBaseStr());
                //System.out.println("weaponType other bonus: " + _statsBonusDamage);
            }
        } else if (attacker instanceof L1NpcInstance) {
            _npc = (L1NpcInstance) attacker;
            if (target instanceof L1PcInstance) {
                _targetPc = (L1PcInstance) target;
                _calcType = NPC_PC;
            } else if (target instanceof L1NpcInstance) {
                _targetNpc = (L1NpcInstance) target;
                _calcType = NPC_NPC;
            }
        }
        _target = target;
        _targetId = target.getId();
        _targetX = target.getX();
        _targetY = target.getY();
    }

    // hit detection
    public boolean calcHit() {
    	if (Config.STANDBY_SERVER) {
    		_isHit = false;
			return _isHit;
		}
    	
        if (_calcType == PC_PC || _calcType == PC_NPC) {
            if (_pc == null || _target == null)
                return _isHit;
            // In case of key link, ignore in case of opponent absolute
            if (_weaponType2 == 17) {
                if (_target.hasSkillEffect(L1SkillId.ABSOLUTE_BARRIER)) {
                    _isHit = false;
                } else if (!_pc.glanceCheck(_targetX, _targetY)) {
					_isHit = false;
                } else if ((_weaponType == 20 || _weaponType == GAUNTLET) && _target.hasSkillEffect(L1SkillId.MOBIUS)) {
                	_isHit = false;
				} else {
                    _isHit = true;
                }
                return _isHit;
            }
            if (_pc instanceof L1RobotInstance && _pc.isElf()) {
                if (!_pc.getLocation().isInScreen(_target.getLocation())) {
                    _isHit = false;
                    return _isHit;
                }
            }
            if (!(_pc instanceof L1RobotInstance) && _weaponType == 20 && _weaponId != 190 && _weaponId != 10000
                    && _weaponId != 202011 && _arrow == null) {
                _isHit = false; // If there is no arrow, make a mistake
            } else if (_weaponType == GAUNTLET && _sting == null) {
                _isHit = false; // If there is no sting, make a mistake
            } else if (!_pc.glanceCheck(_targetX, _targetY)) {
                _isHit = false; // Obstacle determination if the attacker is a player
            } else if (_weaponId == 247 || _weaponId == 248 || _weaponId == 249) {
                _isHit = false; // Trial swords B to C attack invalid
            } else if (_pc.getMapId() == 631 || _pc.getMapId() == 514) {
                _isHit = false;
            } else if (_calcType == PC_PC) {
                _isHit = calcPcPcHit();
                if (_isHit == false) {
                    _pc.sendPackets(new S_SkillSound(_target.getId(), 13418));
                    _targetPc.sendPackets(new S_SkillSound(_target.getId(), 13418)); // effect
                }
            } else if (_calcType == PC_NPC) {
                /** Barpoban opening prevention**/
                if (_pc.baphomettRoom != true && _pc.getX() == 32758 && _pc.getY() == 32878 && _pc.getMapId() == 2) {
                    return _isHit = false;
                } else if (_pc.baphomettRoom != true && _pc.getX() == 32794 && _pc.getY() == 32790
                        && _pc.getMapId() == 2) {
                    return _isHit = false;
                } else {
                    _isHit = calcPcNpcHit();
                }
                // /** バーポバン開け防止 **/
                // if (_isHit == false) {
                // _pc.sendPackets(new S_SkillSound(_targetNpc.getId(),
                // 13418));// ミスエフェクト
                // }
            }
        } else if (_calcType == NPC_PC) {
            _isHit = calcNpcPcHit();
            if (_isHit == false) {
                _targetPc.sendPackets(new S_SkillSound(_target.getId(), 13418)); // effect
            }
        } else if (_calcType == NPC_NPC) {
            _isHit = calcNpcNpcHit();
        } else if (_targetNpc.getNpcTemplate().get_gfxid() == 7684 && !_pc.hasSkillEffect(PAP_FIVEPEARLBUFF)) {
            _isHit = false;
            return _isHit;
        } else if (_targetNpc.getNpcTemplate().get_gfxid() == 7805 && !_pc.hasSkillEffect(PAP_MAGICALPEARLBUFF)) {
            _isHit = false;
            return _isHit;
        }
        return _isHit;
    }

    // Collision detection from player to player
    /*
     * Hit rate to PC = (PC Lv + class correction + STR correction + DEX correction + weapon correction + number of DAI / 2
     * + Magic correction) × 0.68-10 The value calculated by this is the AC of the other PC that you can give the maximum hit (95%) from there.
     * 1 Frequently subtract 1 from the hit rate at least 5% hit rate, maximum hit rate 95%
     */
    private boolean calcPcPcHit() {
        if (_pc.hasSkillEffect(L1SkillId.ABSOLUTE_BLADE)) {
            if (_target.hasSkillEffect(ABSOLUTE_BARRIER)) {
                int chance = _pc.getLevel() - 79;
                if (chance >= 10)
                    chance = 10;
                if (chance >= _random.nextInt(100) + 1) {
                    _targetPc.removeSkillEffect(ABSOLUTE_BARRIER);
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 14539));
                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 14539));
                }
            }
        }
        if (_targetPc.hasSkillEffect(ABSOLUTE_BARRIER) || _targetPc.hasSkillEffect(ICE_LANCE))
            return false;
        
        if (_targetPc.hasSkillEffect(L1SkillId.MOBIUS)) {
        	if (_weaponType == BOW || _weaponType == GAUNTLET) {
                return false;
        	}
        }
        
        // jack-o-lantern safe zone
        if (_targetPc.getMapId() == 612) {
        	if (_targetPc.getLocation().getX() >= 32765
        			&& _targetPc.getLocation().getX() <= 32775
        			&& _targetPc.getLocation().getY() >= 32823
        			&& _targetPc.getLocation().getY() <= 32833) {
        		return false;
        	}
        }
        
        // arrows
        if (_arrow != null) {
        	if (_arrow.getItem().getItemId() == 820014
        			|| _arrow.getItem().getItemId() == 820015
        			|| _arrow.getItem().getItemId() == 820016
        			|| _arrow.getItem().getItemId() == 820017) { // elemental battle arrow
        		_hitRate += 3;
        	}
        }

        /** Battle zone **/
        if (_calcType == PC_PC) {
            if (_pc.getMapId() == 5153) {
                if (_pc.get_DuelLine() == _targetPc.get_DuelLine()) {
                    return false;
                }
            }
        }

        //_hitRate = _pc.getLevel() / 5;
        _hitRate += pcAddHit();
        
        int attackerDice = _random.nextInt(10) + _hitRate;
        // Target PC avoidance skill
        attackerDice += toPcSkillHit();

        int defenderValue = (int) (_targetPc.getAC().getAc() * 0.8) * -1;
        int levelBonus = (int) ((_targetPc.getLevel() - _pc.getLevel()));
        if (levelBonus <= 0)
            levelBonus = 0;

        defenderValue += levelBonus;

        /** DefenderDice Calculation **/
        int defenderDice = toPcDD(defenderValue);

        /** Hit final operation **/
        if (hitRateCal(attackerDice, defenderDice, _hitRate - 10, _hitRate + 10)) {
            return false;
        }

        if (_pc.getLocation().getLineDistance(_targetPc.getLocation()) >= 3 && _weaponType != 20 && _weaponType != GAUNTLET) {
            _hitRate = 0;
        }
        
        int rnd = _random.nextInt(100) + 1; // hit chance controller
        
        // In the case of a bow, even if it hits, avoid it from the ER again.
        if (_weaponType == BOW && _hitRate > rnd) {
            return calcErEvasion();
        }
        
        // melee avoidance based off DG
        if (_weaponType != BOW || _weaponType != GAUNTLET || _weaponType2 != 17) {
		    if (_targetPc.getDg() < 0) {
			    int chance = ThreadLocalRandom.current().nextInt(100) + 1;
		        int dg = _targetPc.getDg() * 10;
		        int positiveDodge = Math.abs(dg);
		        int targetAc = (_targetPc.getAC().getAc() + 100) / 20;
		        int positiveNumber = Math.abs(targetAc);
		        
	            if (positiveNumber > 0) {
	            	positiveDodge += targetAc;
	            }
	            if (chance <= positiveDodge) {
	                return false;
	            }
		    } else if (_targetPc.getDg() > 0) { // dodge will be negative
		    	_hitRate += _targetPc.getDg() * 1;
		    }
		}

        int _jX = _pc.getX() - _targetPc.getX();
        int _jY = _pc.getY() - _targetPc.getY();

        if (_weaponType == 24) { // window
            if ((_jX > 3 || _jX < -3) && (_jY > 3 || _jY < -3)) {
                _hitRate = 0;
            }
        } else if (_weaponType == 20 || _weaponType == GAUNTLET) { // bow
            if ((_jX > 15 || _jX < -15) && (_jY > 15 || _jY < -15)) {
                _hitRate = 0;
            }
        } else {
            if ((_jX > 2 || _jX < -2) && (_jY > 2 || _jY < -2)) {
                _hitRate = 0;
            }
        }

        if (_hitRate >= rnd) {
            return true;
        } else {
            return false;
        }
    }

    // Hit calc from player to NPC
    private boolean calcPcNpcHit() {
        // SPR check
        if (_pc.AttackSpeedCheck2 >= 1) {
            if (_pc.AttackSpeedCheck2 == 1) {
                _pc.AttackSpeed2 = System.currentTimeMillis();
                _pc.sendPackets(new S_SystemMessage("\\fY[Check Start]"));
            }
            _pc.AttackSpeedCheck2++;
            if (_pc.AttackSpeedCheck2 >= 12) {
                _pc.AttackSpeedCheck2 = 0;
                double k = (System.currentTimeMillis() - _pc.AttackSpeed2) / 10D;
                String s = String.format("%.0f", k);
                _pc.AttackSpeed2 = 0;
                _pc.sendPackets(new S_ChatPacket(_pc, "-----------------------------------------"));
                _pc.sendPackets(new S_ChatPacket(_pc, "この変身は" + s + "この攻撃速度に適切な値です。"));
                _pc.sendPackets(new S_ChatPacket(_pc, "-----------------------------------------"));
            }
        }
        // SPR check

        //_hitRate = _pc.getLevel();
        _hitRate += pcAddHit();

        if (_targetNpc.getAc() < 0) {
            int acrate = _targetNpc.getAc() * -1;
            double aaaa = (_hitRate / 100) * (acrate / 2.5D);
            _hitRate -= (int) aaaa;
        }

        if (_pc.getLevel() < _targetNpc.getLevel()) {
            _hitRate -= _targetNpc.getLevel() - _pc.getLevel();
        }
        if (_hitRate > 95) {
            _hitRate = 95;
        } else if (_hitRate < 5) {
            _hitRate = 5;
        }

        int _jX = _pc.getX() - _targetNpc.getX();
        int _jY = _pc.getY() - _targetNpc.getY();

        if (_weaponType == 24) { // When in a window
            if ((_jX > 3 || _jX < -3) && (_jY > 3 || _jY < -3)) {
                _hitRate = 0;
            }
        } else if (_weaponType == 20 || _weaponType == GAUNTLET) { // When it's a bow
            if ((_jX > 15 || _jX < -15) && (_jY > 15 || _jY < -15)) {
                _hitRate = 0;
            }
        } else {
            if ((_jX > 2 || _jX < -2) && (_jY > 2 || _jY < -2)) {
                _hitRate = 0;
            }
        }

        int npcId = _targetNpc.getNpcTemplate().get_npcId(); // Shem Redi error
        if (npcId >= 45912 && npcId <= 45915 && !_pc.hasSkillEffect(STATUS_HOLY_WATER)) {
            _hitRate = 0;
        }
        if (npcId == 45916 && !_pc.hasSkillEffect(STATUS_HOLY_MITHRIL_POWDER)) {
            _hitRate = 0;
        }
        if (npcId == 45941 && !_pc.hasSkillEffect(STATUS_HOLY_WATER_OF_EVA)) {
            _hitRate = 0;
        }
//        if (npcId == 45752 && !_pc.hasSkillEffect(STATUS_CURSE_BARLOG)) {
//            _hitRate = 0;
//        }
//        if (npcId == 45753 && !_pc.hasSkillEffect(STATUS_CURSE_BARLOG)) {
//            _hitRate = 0;
//        }
//        if (npcId == 45675 && !_pc.hasSkillEffect(STATUS_CURSE_YAHEE)) {
//            _hitRate = 0;
//        }
//        if (npcId == 81082 && !_pc.hasSkillEffect(STATUS_CURSE_YAHEE)) {
//            _hitRate = 0;
//        }
//        if (npcId == 45625 && !_pc.hasSkillEffect(STATUS_CURSE_YAHEE)) {
//            _hitRate = 0;
//        }
//        if (npcId == 45674 && !_pc.hasSkillEffect(STATUS_CURSE_YAHEE)) {
//            _hitRate = 0;
//        }
//        if (npcId == 45685 && !_pc.hasSkillEffect(STATUS_CURSE_YAHEE)) {
//            _hitRate = 0;
//        }
        if (npcId >= 46068 && npcId <= 46091 && _pc.getTempCharGfx() == 6035) {
            _hitRate = 0;
        }
        if (npcId >= 46092 && npcId <= 46106 && _pc.getTempCharGfx() == 6034) {
            _hitRate = 0;
        }
        if (_targetNpc.getNpcTemplate().get_gfxid() == 7684 && !_pc.hasSkillEffect(PAP_FIVEPEARLBUFF)) { // 五色真珠
            _hitRate = 0;
        }
        if (_targetNpc.getNpcTemplate().get_gfxid() == 7805 && !_pc.hasSkillEffect(PAP_MAGICALPEARLBUFF)) { // Mysterious pearl
            _hitRate = 0;
        }

        return _hitRate >= _random.nextInt(100) + 1;
    }

    // Collision detection from NPC to player
    private boolean calcNpcPcHit() {
        if (_targetPc.hasSkillEffect(ABSOLUTE_BARRIER) || _targetPc.hasSkillEffect(MOBIUS)) {
            return false;
        }
        _hitRate += _npc.getLevel();// * 1.1;

        if (_npc instanceof L1PetInstance) { // Pets add to LV1 Hit +2
            _hitRate += _npc.getLevel() * 2;
            _hitRate += ((L1PetInstance) _npc).getHitByWeapon();
        }

        _hitRate += _npc.getHitup();

        int attackerDice = _random.nextInt(20) + 1 + _hitRate - 1;

        /** Target PC avoidance skill calculation **/
        attackerDice += toPcSkillHit();

        int defenderValue = (_targetPc.getAC().getAc()) * -1;

        /** DefenderDice Calculation **/
        int defenderDice = toPcDD(defenderValue);

        /** Hit final operation **/
        if (hitRateCal(attackerDice, defenderDice, _hitRate, _hitRate + 19))
            return false;

        int rnd = _random.nextInt(100) + 1;

        // If the NPC's attack range is 10 or more and is 2 or more away, it is considered a bow attack.
        if (_npc.getNpcTemplate().get_ranged() >= 10 && _hitRate > rnd
                && _npc.getLocation().getTileLineDistance(new Point(_targetX, _targetY)) >= 2) {
            return calcErEvasion();
        }
        
        // melee avoidance
		if (_npc.getNpcTemplate().get_ranged() <= 3 && _npc.getLocation().getTileLineDistance(new Point(_targetX, _targetY)) <= 3) {
			// melee avoidance according to DG value
		    if (_targetPc.getDg() < 0) {
			    int chance = ThreadLocalRandom.current().nextInt(100) + 1;
		        int dg = _targetPc.getDg() * 10;
		        int positiveDodge = Math.abs(dg);
		        int targetAc = (_targetPc.getAC().getAc() + 100) / 20;
		        int positiveNumber = Math.abs(targetAc);
		        
	            if (positiveNumber > 0) {
	            	positiveDodge += targetAc;
	            }
	            if (chance <= positiveDodge) {
	                return false;
	            }
		    } else if (_targetPc.getDg() > 0) { // if negative
		    	_hitRate += _targetPc.getDg() * -1;
		    }
		}
		
        if (_hitRate >= rnd) {
            return true;
        } else {
            return false;
        }
    }

    // Hit from NPC to NPC
    private boolean calcNpcNpcHit() {
        int target_ac = 10 - _targetNpc.getAC().getAc();
        int attacker_lvl = _npc.getNpcTemplate().get_level();

        if (target_ac != 0) {
            _hitRate = (100 / target_ac * attacker_lvl); // 100% accuracy when attacked AC = attack Lv
        } else {
            _hitRate = 100 / 1 * attacker_lvl;
        }

        if (_npc instanceof L1PetInstance) { // Pets add to LV1 Hit +2
            _hitRate += _npc.getLevel() * 2;
            _hitRate += ((L1PetInstance) _npc).getHitByWeapon();
        }

        if (_hitRate < attacker_lvl) {
            _hitRate = attacker_lvl; // Minimum hit rate = Lv%
        }
        if (_hitRate > 95) {
            _hitRate = 95; // Maximum hit rate is 95%
        }
        if (_hitRate < 5) {
            _hitRate = 5; // Hit rate 5% when attacker Lv is less than 5
        }

        int rnd = _random.nextInt(100) + 1;
        return _hitRate >= rnd;
    }

    // ranged avoidance by ER 
    private boolean calcErEvasion() {
        int evasionRate = _targetPc.get_PlusEr();
        int rnd = _random.nextInt(100) + 1;
        return evasionRate < rnd;
    }

    /* Damage calculation */

    public int calcDamage() {
        try {
            switch (_calcType) {
            case PC_PC:
                _damage = calcPcPcDamage();

                if (_weaponType != 20 && _weaponType != GAUNTLET && _weaponType2 != 17 && _weaponType2 != 19) { // Activate if no bow
                    if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), L1SkillId.PASSIVE_TITAN_ROCK)) {
                        int percentCurrentHP = (int) Math.round(((double) _targetPc.getCurrentHp() / (double) _targetPc.getMaxHp()) * 100);
                        int percentHPtoStartPassive = 40;
                        int randomChance = ThreadLocalRandom.current().nextInt(100) + 1;
                        int hpBonusToStart = 0;
                        int procRate = 12;
                        int targetLevel = _target.getLevel();
                        
                        if (_target.getTitanPassiveUp() != 0) {
                            hpBonusToStart += _target.getTitanPassiveUp();
                        }
                        
                        if (targetLevel >= 90) {
                        	if (targetLevel >= 98)
                        		procRate += 8;
                        	else
                        		procRate += targetLevel - 90;
                        }
                        
                        if (_targetPc.isWarrior() && _targetPc.getEquipSlot().getWeaponCount() == 2) {
                            for (L1ItemInstance item2 : _targetPc.getInventory().getItems()) {
                                if (item2 != null && item2.getItem().getType2() == 1 && item2.getItem().getType() == 6 && item2.isEquipped()) {
                                    if (item2.getItemId() == 202014) { // Titan's Rage
                                        hpBonusToStart += 5;
                                    }
                                }
                            }
                        } else {
                            for (L1ItemInstance item : _targetPc.getInventory().getItems()) {
                                if (_targetPc.getWeapon().equals(item)) {
                                    if (item.isEquipped()) {
                                        if (item.getItemId() == 202014) { // 
                                            hpBonusToStart += 5;
                                        }
                                    }
                                }
                            }
                        }
                        if (percentCurrentHP <= (percentHPtoStartPassive + hpBonusToStart + _targetPc.getTitanRisingBonus()) && randomChance <= procRate) {
                            if (_targetPc.getInventory().checkItem(41246, 5)) { // crystal cost
                                _pc.receiveCounterBarrierDamage(_targetPc, calcTitanDamage());
                                _damage = 0;
                                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12555));
                                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 12555));
                                _targetPc.getInventory().consumeItem(41246, 5);
                            } else {
                                _targetPc.sendPackets(new S_SystemMessage("\\aBYou require more Crystal for Titan passives."));
                            }
                        }
                    }
                } else {
                    if (_weaponType2 != 17 && _weaponType2 != 19) {
                        if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), L1SkillId.PASSIVE_TITAN_BLITZ)) {
                            int percentWarriorHP = (int) Math.round(((double) _targetPc.getCurrentHp() / (double) _targetPc.getMaxHp()) * 100);
                            int percentHPtoStartPassive = 40;
                            int randomChance = ThreadLocalRandom.current().nextInt(100) + 1;
                            int hpBonusToStart = 0;
                            
                            if (_target.getTitanPassiveUp() != 0) {
                                hpBonusToStart += _target.getTitanPassiveUp();
                            }
                            if (_targetPc.isWarrior() && _targetPc.getEquipSlot().getWeaponCount() == 2) {

                                for (L1ItemInstance item2 : _targetPc.getInventory().getItems()) {
                                    if (item2 != null && item2.getItem().getType2() == 1 && item2.getItem().getType() == 6 && item2.isEquipped()) {
                                        if (item2.getItemId() == 202014) {
                                            hpBonusToStart += 5;
                                        }
                                    }
                                }
                            } else {
                                for (L1ItemInstance item : _targetPc.getInventory().getItems()) {
                                    if (_targetPc.getWeapon().equals(item)) {
                                        if (item.isEquipped()) {
                                            if (item.getItemId() == 202014) {
                                                hpBonusToStart += 5;
                                            }
                                        }
                                    }
                                }
                            }

                            if (percentWarriorHP <= (percentHPtoStartPassive + hpBonusToStart + _targetPc.getTitanRisingBonus()) && randomChance <= 12) {
                                if (_targetPc.getInventory().checkItem(41246, 5)) {
                                    _pc.receiveCounterBarrierDamage(_targetPc, calcTitanDamage());
                                    _damage = 0;
                                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12557));
                                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 12557));
                                    _targetPc.getInventory().consumeItem(41246, 5);
                                } else {
                                    _targetPc.sendPackets(new S_SystemMessage("\\aBYou require more Crystal for Titan passives."));
                                }
                            }
                        }
                    } else { // If not melee or range, check magic
                        if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), L1SkillId.PASSIVE_TITAN_MAGIC)) {
                            int percentCurrentHP = (int) Math.round(((double) _targetPc.getCurrentHp() / (double) _targetPc.getMaxHp()) * 100);
                            int percentHPtoStartPassive = 40;
                            int randomChance = ThreadLocalRandom.current().nextInt(100) + 1;
                            int minHpBonusToStart = 0;
                            
                            if (_target.getTitanPassiveUp() != 0) {
                                minHpBonusToStart += _target.getTitanPassiveUp();
                            }

                            if (_targetPc.isWarrior() && _targetPc.getEquipSlot().getWeaponCount() == 2) {
                                for (L1ItemInstance item2 : _targetPc.getInventory().getItems()) {
                                    if (item2 != null && item2.getItem().getType2() == 1 && item2.getItem().getType() == 6 && item2.isEquipped()) {
                                        if (item2.getItemId() == 202014) {
                                            minHpBonusToStart += 5;
                                        }
                                    }
                                }
                            } else {
                                for (L1ItemInstance item : _targetPc.getInventory().getItems()) {
                                    if (_targetPc.getWeapon().equals(item)) {
                                        if (item.isEquipped()) {
                                            if (item.getItemId() == 202014) {
                                                minHpBonusToStart += 5;
                                            }
                                        }
                                    }
                                }
                            }

                            if (percentCurrentHP <= (percentHPtoStartPassive + minHpBonusToStart + _targetPc.getTitanRisingBonus()) && randomChance <= 12) {
                                if (_targetPc.getInventory().checkItem(41246, 5)) {
                                    if (_calcType == 1)
                                        _pc.receiveCounterBarrierDamage(_targetPc, calcTitanDamage());
                                    else if (_calcType == 2)
                                        _npc.receiveCounterBarrierDamage(_targetPc, calcTitanDamage());
                                    _damage = 0;
                                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12559));
                                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 12559));
                                    _targetPc.getInventory().consumeItem(41246, 5);
                                } else {
                                	_targetPc.sendPackets(new S_SystemMessage("\\aBYou require more crystals for Titan passives."));
                                }
                            }
                        }
                    }
                }

                break;
            case PC_NPC:
                _damage = calcPcNpcDamage();
                break;
            case NPC_PC:
                _damage = calcNpcPcDamage();

                int bowactid = _npc.getNpcTemplate().getBowActId();
                if (bowactid != 66) {
                    if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), L1SkillId.PASSIVE_TITAN_ROCK)) {
                    	int percentCurrentHP = (int) Math.round(((double) _targetPc.getCurrentHp() / (double) _targetPc.getMaxHp()) * 100);
                        int percentHPtoStartPassive = 40;
                        int randomChance = ThreadLocalRandom.current().nextInt(100) + 1;
                        int bonusHPtoStart = 0;
                        int targetLevel = _targetPc.getLevel();
                        int procRate = 12;
                        
                        if (_target.getTitanPassiveUp() != 0) {
                        	bonusHPtoStart += _target.getTitanPassiveUp();
                        }
                        
                        if (targetLevel >= 90) { // new titan rock bonus
                        	if (targetLevel >= 98)
                        		procRate += 8;
                        	else
                        		procRate += targetLevel - 90;
                        }

                        if (_targetPc.isWarrior() && _targetPc.getEquipSlot().getWeaponCount() == 2) {
                            for (L1ItemInstance item2 : _targetPc.getInventory().getItems()) {
                                if (item2 != null && item2.getItem().getType2() == 1 && item2.getItem().getType() == 6 && item2.isEquipped()) {
                                    if (item2.getItemId() == 202014) {
                                    	bonusHPtoStart += 5;
                                    }
                                }
                            }
                        } else {
                            for (L1ItemInstance item : _targetPc.getInventory().getItems()) {
                                if (_targetPc.getWeapon().equals(item)) {
                                    if (item.isEquipped()) {
                                        if (item.getItemId() == 202014) {
                                        	bonusHPtoStart += 5;
                                        }
                                    }
                                }
                            }
                        }
                        if (percentCurrentHP <= (percentHPtoStartPassive + bonusHPtoStart + _targetPc.getTitanRisingBonus()) && randomChance <= procRate) {
                            if (_targetPc.getInventory().checkItem(41246, 5)) {
                                _npc.receiveCounterBarrierDamage(_targetPc, calcTitanDamage());
                                _damage = 0;
                                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12555));
                                _targetPc.getInventory().consumeItem(41246, 5);
                            } else {
                            	_targetPc.sendPackets(new S_SystemMessage("\\aBYou require more crystals for Titan passives."));
                            }
                        }
                    }
                } else {
                    if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), L1SkillId.PASSIVE_TITAN_BLITZ)) {
                        int percentCurrentHP = (int) Math.round(((double) _targetPc.getCurrentHp() / (double) _targetPc.getMaxHp()) * 100);
                        int percentHPtoStartPassive = 40;
                        int randomChance = _random.nextInt(100) + 1;
                        int minHpBonusToStart = 0;
                        
                        if (_target.getTitanPassiveUp() != 0) {
                            minHpBonusToStart += _target.getTitanPassiveUp();
                        }
                        
                        if (_targetPc.isWarrior() && _targetPc.getEquipSlot().getWeaponCount() == 2) {
                            for (L1ItemInstance item2 : _targetPc.getInventory().getItems()) {
                                if (item2 != null && item2.getItem().getType2() == 1 && item2.getItem().getType() == 6 && item2.isEquipped()) {
                                    if (item2.getItemId() == 202014) {
                                        minHpBonusToStart += 5;
                                    }
                                }
                            }
                        } else {
                            for (L1ItemInstance item : _targetPc.getInventory().getItems()) {
                                if (_targetPc.getWeapon().equals(item)) {
                                    if (item.isEquipped()) {
                                        if (item.getItemId() == 202014) {
                                            minHpBonusToStart += 5;
                                        }
                                    }
                                }
                            }
                        }

                        if (percentCurrentHP <= (percentHPtoStartPassive + minHpBonusToStart + _targetPc.getTitanRisingBonus()) && randomChance <= 12) {
                            if (_targetPc.getInventory().checkItem(41246, 5)) {
                                _npc.receiveCounterBarrierDamage(_targetPc, calcTitanDamage());
                                _damage = 0;
                                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12557));
                                _targetPc.getInventory().consumeItem(41246, 5);
                            } else {
                            	_targetPc.sendPackets(new S_SystemMessage("\\aBYou require more crystals for Titan passives."));
                            }
                        }
                    }
                }
                break;
            case NPC_NPC:
                _damage = calcNpcNpcDamage();
                break;
            default:
                break;
            }
        } catch (Exception e) {
        }
        return _damage;
    }

    // Damage calculation from player to player
    public int calcPcPcDamage() {
        int maxWeaponSmallDamage = _weaponSmall;
		int weaponDamageOut = 0;
		int weaponTotalBonus = _weaponEnchant + _weaponAddDmg;

        if ((_pc.getZoneType() == 1 && _targetPc.getZoneType() == 0)
                || (_pc.getZoneType() == 1 && _targetPc.getZoneType() == -1)) {
            _isHit = false;
            // Normal/combat zone attack not possible in safety zone
        }

        // over enchant buffs 2018
        if (_weaponType != 0) {
            if (_weaponId != ARROW) {
                switch (weapon.getEnchantLevel()) {
                case 10:
                	weaponTotalBonus += 1;
                    break;
                case 11:
                	weaponTotalBonus += 2;
                    break;
                case 12:
                	weaponTotalBonus += 3;
                    break;
                case 13:
                	weaponTotalBonus += 4;
                    break;
                case 14:
                	weaponTotalBonus += 5;
                    break;
                case 15:
                	weaponTotalBonus += 6;
                    break;
                default:
                	if (weapon.getEnchantLevel() >= 16) {
                		weaponTotalBonus += 6; // Handle cases 16 and above
                    }
                    break;
                }
            }
        }
        
        if (_weaponType != BOW && _weaponType != GAUNTLET) {
        	weaponDamageOut += _statsBonusDamage + _pc.getDmgup();
        } else {
        	weaponDamageOut += _statsBonusDamage + _pc.getBowDmgup();
        }
        
        if (_weaponId == 203018) { // Roar Edoryu
            _weaponDoubleDmgChance += _pc.getWeapon().getEnchantLevel();
        }
        if (_weaponType == 58) { // claw
            int clawProcChance = _random.nextInt(100) + 1;
            if (clawProcChance <= _weaponDoubleDmgChance) {
                weaponDamageOut = maxWeaponSmallDamage + weaponTotalBonus;
                _pc.sendPackets(new S_SkillSound(_pc.getId(), 3671));
                _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 3671));
            } else {
            	weaponDamageOut = (_random.nextInt(maxWeaponSmallDamage)) + weaponTotalBonus;
            }
        } else if (_weaponType == 0) { // bare hands
            weaponDamageOut = 1;
        } else {
            weaponDamageOut += (_random.nextInt(maxWeaponSmallDamage)) + weaponTotalBonus;
        }

        if (_pc.hasSkillEffect(SOUL_OF_FLAME)) {
            if (_weaponType != 20 && _weaponType != GAUNTLET) {
                weaponDamageOut += maxWeaponSmallDamage + weaponTotalBonus;
            }
        }
        
        /** Blessing book Weapon ivy related **/
        /*
         * if (_weaponType != 0) { 
         * if (weapon.getBless() == 0 || weapon.getBless() == 128) { 
         * weaponDamage += 3;
         * } 
         * }
         */
        
       if (_weaponType != 0) { // not fists
            if (_weaponType != 20 && _weaponType != GAUNTLET && _weaponType != 17) { // not ranged or kiringku
                int bonusChanceToCrit = CalcStat.calcChanceToCritSTR(_pc.getAbility().getBaseStr()) + _pc.getDmgCritical();
                int calcRandomChanceToCrit = _random.nextInt(100) + 1;

                // Strike of Valakas
                if (_pc.getInventory().checkEquipped(22208)
                		|| _pc.getInventory().checkEquipped(22209)
                        || _pc.getInventory().checkEquipped(22210)
                        || _pc.getInventory().checkEquipped(22211)) { // needs increase crit at +7 +8 +9
                    int strikeOfValakasChance = _random.nextInt(100) + 1;
                    if (strikeOfValakasChance <= 3) {
                        weaponDamageOut = maxWeaponSmallDamage + weaponTotalBonus;
                        S_UseAttackSkill packet = new S_UseAttackSkill(_target, _target.getId(), 15841, _targetX, _targetY, ActionCodes.ACTION_Attack, false);
                        _pc.sendPackets(packet);
                        Broadcaster.broadcastPacket(_pc, packet);
                    }
                }
                
                if (calcRandomChanceToCrit <= bonusChanceToCrit) {
                	weaponDamageOut = maxWeaponSmallDamage + weaponTotalBonus;
                    _isCritical = true;
                }
            } else {
                int bowBonusChanceToCrit = CalcStat.calcBowCritical(_pc.getAbility().getBaseDex()) + _pc.getBowDmgCritical();
                int calcBowRandomChanceToCrit = _random.nextInt(100) + 1;
                
                if (_pc.hasSkillEffect(EAGLE_EYE)) {
                	bowBonusChanceToCrit += 2;
                }
                
                if (calcBowRandomChanceToCrit <= bowBonusChanceToCrit) {
					weaponDamageOut = maxWeaponSmallDamage + weaponTotalBonus;
                    _isCritical = true;
                }
            }
        }

        if (_weaponType == 54 && _pc.hasSkillEffect(L1SkillId.ASSASSIN)) {
            if (!_pc.getInventory().checkEquipped(20077) && !_pc.getInventory().checkEquipped(120077) && !_pc.getInventory().checkEquipped(20062)) {
                if (_random.nextInt(100) + 1 <= 60) {
                	weaponDamageOut *= 2.5;
                    _pc.sendPackets(new S_SkillSound(_pc.getId(), 14547));
                    _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 14547));

                    if (SkillsTable.getInstance().spellCheck(_pc.getId(), 241)) {
                        int time = 3 + (_pc.getLevel() - 85);
                        if (time > 8)
                            time = 8;
                        _pc.setSkillEffect(L1SkillId.BLAZING_SPIRITS, time * 1000);
                        _pc.sendPackets(new S_NewSkillIcon(L1SkillId.BLAZING_SPIRITS, true, time));
                    }
                }
            } else {
                _pc.sendPackets(new S_SystemMessage("This skill cannot be activated in a transparent state."));
            }
            _pc.removeSkillEffect(L1SkillId.ASSASSIN);
        }

        if (_weaponType == EDORYU && _pc.isDarkelf()) {
            if (_pc.hasSkillEffect(L1SkillId.BLAZING_SPIRITS)) {
            	weaponDamageOut *= 2.5;
                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 14547));
                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 14547));
                _pc.sendPackets(new S_AttackCritical(_pc, _targetId, 54));
                Broadcaster.broadcastPacket(_pc, new S_AttackCritical(_pc, _targetId, 54));
            } else if ((_random.nextInt(100) + 1) <= (_weaponDoubleDmgChance - weapon.get_durability())) {
            	weaponDamageOut *= 2;
                _pc.sendPackets(new S_SkillSound(_pc.getId(), 3398));
                _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 3398));
            }
        }

        if (_pc.hasSkillEffect(DOUBLE_BREAK) && (_weaponType == EDORYU || _weaponType == KIRI_CLAW)) { // Double brake probability
			int rnd = 25;
			switch (_pc.getLevel()) {
			case 98:
				rnd += 5;
				break;
			case 96:
				rnd += 4;
				break;
			case 94:
				rnd += 3;
				break;
			case 92:
				rnd += 2;
				break;
			case 90:
				rnd += 1;
				break;
			default:
				rnd = 25;
				break;
			}
			
			if ((_random.nextInt(100) + 1) <= rnd) { // PvP
				weaponDamageOut *= 2;
				if (_pc.hasSkillEffect(BURNING_SPIRIT)) {
					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 6532));
					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 6532));
					}
				}
			}

        double dmg = weaponDamageOut;

        if (_weaponType2 == 17) { // Kiringku PvP
        	int dmgFromInt = _pc.getAbility().getTotalInt();
        	int dmgFromSP = _pc.getAbility().getSp();
        	int kiringkuRandom = (ThreadLocalRandom.current().nextInt(dmgFromInt) + 4);
        	int randomChance = (ThreadLocalRandom.current().nextInt(100) + 1);
        	
        	dmg += kiringkuRandom + dmgFromSP;
        	
        	int magicCritical = CalcStat.calcINTMagicCritical(_pc.getAbility().getTotalInt()) + _pc.getMagicBonus();
        	//int magicCritical = CalcStat.calcINTMagicCritical(_pc.getAbility().getTotalInt()) + 1;
        	if (randomChance <= magicCritical) {
				dmg *= 1.25;
				_isCritical = true;
			} else {
				dmg = calcMrDefense(_target.getResistance().getEffectedMrBySkill(), dmg);
			}
        }

        if (_weaponType == BOW) {
            if (_arrow != null) {
                int add_dmg = _arrow.getItem().getDmgSmall();
                if (add_dmg == 0) {
                    add_dmg = 1;
                }
                dmg = dmg + _random.nextInt(add_dmg) + 1;
            } else if (_weaponId == 190) { // Saiha's bow
                dmg = dmg + 2;

            } else if (_pc.getTempCharGfx() == 7959) { // Heavenly bow
                dmg = dmg + _random.nextInt(10);
            }
        } else if (_pc.getTempCharGfx() == 202011) { // Gaia's rage
            dmg = dmg + _random.nextInt(15);

        } else if (_weaponType == GAUNTLET) { // Cancer tote red
            int add_dmg = _sting.getItem().getDmgSmall();
            if (add_dmg == 0) {
                add_dmg = 1;
            }
            dmg = dmg + _random.nextInt(add_dmg) + 1;
        }
        
        // arrows
        if (_arrow != null) {
        	if (_arrow.getItem().getItemId() == 820014
        			|| _arrow.getItem().getItemId() == 820015
        			|| _arrow.getItem().getItemId() == 820016
        			|| _arrow.getItem().getItemId() == 820017
        			|| _arrow.getItem().getItemId() == 40744) { // hunters silver arrow
        		dmg += 3;
        	} else if (_arrow.getItem().getItemId() == 40743) { // hunters arrow
        		dmg += 1;
        	}
        }

        /** Red Knight's Great Sword Renewal **/
        if (_pc.getInventory().checkEquipped(202002) || _pc.getInventory().checkEquipped(203002)
                || _pc.getInventory().checkEquipped(1136) || _pc.getInventory().checkEquipped(1137)) {
            if (_pc.getLawful() < -32760) {
                dmg += 8;
            }
            if (_pc.getLawful() >= -32760 && _pc.getLawful() < -25000) {
                dmg += 6;
            }
            if (_pc.getLawful() >= -25000 && _pc.getLawful() < -15000) {
                dmg += 4;
            }
            if (_pc.getLawful() >= -15000 && _pc.getLawful() < 0) {
                dmg += 2;
            }
        }
        
        // bonus damage from lawful/chaotic
        dmg += _pc.getBapodmg();
        //System.out.println("lawful: " + _pc.getBapodmg());
        
        // Burning spirits, elemental fire, brave mental, BLOW_ATTACK 1.5 times skill effect and ivy part
        int skillCriticalBuff = _random.nextInt(100) + 1;
        if (_weaponType != 20 && _weaponType != GAUNTLET && _weaponType2 != 17) {
            if (_pc.hasSkillEffect(ELEMENTAL_FIRE) || _pc.hasSkillEffect(BRAVE_MENTAL) || _pc.hasSkillEffect(QUAKE)) {
                if (skillCriticalBuff <= 12) {
                	if (_calcType == PC_PC) {
    					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 7727), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 7727), true);
    				} else if (_calcType == PC_NPC) {
    					_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 7727), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 7727), true);
    				}
                	dmg *= 1.5;
                }
            } else if (_pc.hasSkillEffect(BURNING_SPIRIT)) { // PvP
            	if (skillCriticalBuff <= 33) {
            		if (_calcType == PC_PC) {
						_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 6532), true);
						Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 6532), true);
					} else if (_calcType == PC_NPC) {
						_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 6532), true);
						Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 6532), true);
					}
            		dmg *= 1.5;
            	}
            } else if (_pc.hasSkillEffect(BLOW_ATTACK)) { // PvP
            	int blowAttackChance = 5; // default 5%
    			if (_pc.getLevel() >= 75) {
    				blowAttackChance += (_pc.getLevel() - 74); // 1% added when level 75 or higher
    			}
    			if (blowAttackChance > 20) {
    				blowAttackChance = 20;
    			}
    			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= blowAttackChance) {
    				if (_calcType == PC_PC) {
    					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 17223), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 17223), true);
    				} else if (_calcType == PC_NPC) {
    					_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 17223), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 17223), true);
    				}
    				dmg *= 1.5;
    			}
            }
        }
        
        if (_pc.hasSkillEffect(CYCLONE) && (_weaponType == 20 || _weaponType == GAUNTLET || _weaponType == 17)) {
			int cycloneProcChance = 5;
			if (_pc.getLevel() > 75) {
				cycloneProcChance = cycloneProcChance + (_pc.getLevel() - 75); // 1% added at level 75 and above
			}
	
			if (cycloneProcChance > 20) { // maximum probability
				cycloneProcChance = 20;
			}
	
			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= cycloneProcChance) {
				if (_calcType == PC_PC) {
					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 17557), true);
					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 17557), true);
				} else if (_calcType == PC_NPC) {
					_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 17557), true);
					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 17557), true);
				}
				dmg *= 1.5;
				//System.out.println("Cyclone " + dmg);
			}
        }
        
        if (_weaponType2 != 17) {
        	dmg -= (calcPcDefense() / 8);
        }
        
        // class damage reduction by level
        if (_targetPc.getLevel() >= 60) {
        	int classDR = 0;
            if (_targetPc.isCrown()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 3);
            } else if (_targetPc.isKnight()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 2);
            } else if (_targetPc.isElf()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 4);
            } else if (_targetPc.isWizard()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 4);
            } else if (_targetPc.isDarkelf()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 3);
            } else if (_targetPc.isDragonknight()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 3);
            } else if (_targetPc.isBlackwizard()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 3);
            } else if (_targetPc.isWarrior()) {
                int levelDiff = _targetPc.getLevel() - 60;
                classDR = 1 + (levelDiff / 3);
            }
            dmg -= classDR;
        }

        switch (_weaponId) {
        case 2: // Dice Dagger
        case 200002: // Cursed Dice Dagger
            dmg = L1WeaponSkill.DiceDagger(_pc, _targetPc, weapon);
            break;
        case 12: // Wind Blade Dagger
			dmg += L1WeaponSkill.LordSword(_pc, _target, 4842, _weaponEnchant);
			ruinGreatSword(dmg);
            break;
        case 54: // Sword of Kurtz
            dmg += L1WeaponSkill.kurtzSword(_pc, _target, _weaponEnchant, 10405);
            break;
        case 58: // Death Knight Flame Blade
            dmg += L1WeaponSkill.deathKnightFireSwordPvP(_pc, _target, _weaponEnchant, 7300);
            break;
        case 61:
        case 294:
		case 202014:
            dmg += L1WeaponSkill.LordSword(_pc, _target, 4842, _weaponEnchant);
            break;
            
        // edoryu
		case 76: // Edoryu of Ronde
			dmg += L1WeaponSkill.revengeProc(_pc, _targetPc, 10145, _weaponEnchant);
			break;
        case 86: // Edoryu of Red Shadow
			L1WeaponSkill.redShadowEdoryu(_pc, _targetPc);
            break;
        case 124: // Staff of Baphomet
            dmg += L1WeaponSkill.staffOfBaphomet(_pc, _target, _weaponEnchant, 129);
            break;
        case 134: // Crystalized Staff
            dmg += L1WeaponSkill.lightningStrike(_pc, _target, _weaponEnchant, 10405);
            break;
        case 204: // Impartial Arbalest
        case 100204: // Crimson Crossbow
            L1WeaponSkill.redShadowEdoryu(_pc, _targetPc);
            break;

        case 307:
        case 308:
        case 309:
        case 310:
        case 311:
        case 313:
        case 314:
            dmg = L1WeaponSkill.blazeShock(_pc, _targetPc, _weaponEnchant);
            break;
        case 291: // demon king kiringku
        case 292: // demon king sword
        case 1010:
        case 1011:
        case 1012:
        case 1013:
        case 1014:
        case 1015: // Demon King Axe
            L1WeaponSkill.getDiseaseWeapon(_pc, _targetPc, _weaponId);
            break;
        case 203020: // Dagger of Life
        case 601: // Great Sword of Destruction
            ruinGreatSword(dmg);
            break;
            
        //kiringku
        case 283: // Valakas Kiringku
            dmg += L1WeaponSkill.valakasWeaponProc(_pc, _target, 10405, _weaponEnchant);
            break;
		case 1112: // Hidden Demon Kiringku
			dmg += evilTrick(_pc, _target, 8152, _weaponEnchant);
			break;
		case 1120: // Cold sensitivity key link
            dmg += L1WeaponSkill.iceColdKiringku(_pc, _target, 6553, _weaponEnchant);
            break;
        case 1135: // Resonance key link
            //dmg += L1WeaponSkill.Kiringku_Resonance(_pc, _target, 5201, _weaponEnchant);
            dmg += L1WeaponSkill.phantomShock(_pc, _target, _weaponEnchant);
			break;
        case 202012: // Hyperion's despair
            dmg += L1WeaponSkill.hyperionsDespair(_pc, _target, 12248, _weaponEnchant);
			break;
        case 1116: // Mysterious wand
        case 1118: // Mysterious longbow
        case 202011: // Gaia's Rage
            dmg += evilTrick(_pc, _target, 8981, _weaponEnchant);
            break;
        case 1109: // Hidden Demon Claw
        case 1113: // Hidden Demon Sword
        case 1114: // Hidden Demon Sword 2hs
        case 1115: // Mysterious Sword
        case 1117: // Mysterious Claw
        case 203011: // Hidden Demon Axe
            dmg += reverseEvil(_pc, _target, 8150, _weaponEnchant);
            break;
        case 1110: // Hidden Demon Staff
        case 1111: // Hidden Demon Bow
            dmg += evilTrick(_pc, _target, 8152, _weaponEnchant);
            break;
        case 1108: // Hidden Demon Chain Sword
            dmg += reverseEvil(_pc, _target, 8150, _weaponEnchant);
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 1119: // Extreme Chain Sword Ice Eruption
			dmg += L1WeaponSkill.iceEruption(_pc, _target, 3685, _weaponEnchant);
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 1123: // Bloodserker
        	L1WeaponSkill.ChainSword(_pc,_target);
            bloodSucker(dmg, _weaponEnchant);
            break;
        case 202013: // Chronos's Fear
            L1WeaponSkill.ChainSword(_pc,_target);
			dmg += L1WeaponSkill.LordSword(_pc, _target, 4842, _weaponEnchant);
            break;
        case 500: // Destructors Chain Sword
        case 501: // Collapsed One's Chain Sword
        case 1104: // Elmore Chain Sword
        //case 1132: // ベビーテルランチェーンソード
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 203017: // Annihalator Chain Sword
            L1WeaponSkill.ChainSword_Destroyer(_pc);
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.annihilate(_pc, _target, 4077, _weaponEnchant);
            break;
        case 203006: // Typhoon Axe
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.hellStormProc(_pc, _target, 7977, _weaponEnchant);
            break;
        case 1136: // Nightmare longbow
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.nightmareProc(_pc, _target, 14339, _weaponEnchant);
            break;
        case 203025: // Jin Fighting Avian Greatsword
        case 203026: // Jin Fighting Avian Greatsword B
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.Jinsa(_pc, _target, 8032, _weaponEnchant);
            break;
        case 312: // Arnold Chain Sword
            dmg = L1WeaponSkill.ChainSword_BlazeShock(_pc, _targetNpc, _weaponEnchant);
            break;
        case 202001: // Chainsword of Illusion
            dmg += L1WeaponSkill.ChainSword_BlazeShock(_pc, _target, _weaponEnchant);
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 1124: // Dual wield of destruction
        case 1125: // Crow of destruction
        case 11125:// Dual wield of blessing destruction
            dmg += L1WeaponSkill.DestructionDualBlade_Crow(_pc, _target, 9359, _weaponEnchant);
            break;
        case 600: // Thunder God Sword
            dmg += L1WeaponSkill.electricShockProc(_pc, _target, 3940, _weaponEnchant);
            break;
        case 604: // Ice Cold wind axe
            dmg += L1WeaponSkill.ExColdWind(_pc, _target, 3704, _weaponEnchant);
            break;
        case 605: // Mad wind ax
        case 203015: // Gale Ax
            dmg += L1WeaponSkill.InsanityWindAx(_pc, _target, 5524, _weaponEnchant);
            break;
        case 191: // Sarkhon's bow
            dmg += L1WeaponSkill.AngelSlayer(_pc, _target, 9361, _weaponEnchant);
            break;
        case 202003: // Zeros Staff
            dmg += L1WeaponSkill.zerosStaff(_pc, _target, _weaponEnchant, 11760);
            break;
        case 450004: // Fonos Bume Smache
        	dmg += L1WeaponSkill.fonosBumeSmache(_pc, _target, _weaponEnchant, 762);
        	break;
        default:
            dmg += L1WeaponSkill.getWeaponSkillDamage(_pc, _target, _weaponId);
            break;
        }

//        if (_weaponType != 17 && _targetPc.hasSkillEffect(MAJESTY)) {
//        	int targetPcLvl = _targetPc.getLevel();
//			if (targetPcLvl < 80) {
//				targetPcLvl = 80;
//			}
//			int dmg2 = (targetPcLvl - 80) + 3;
//			dmg -= dmg2 > 0 ? dmg2 : 0;
//		}
        
//        if (_pc.hasSkillEffect(BLOW_ATTACK) && _weaponType != 20 && _weaponType != GAUNTLET) {
//        	int chance = Config.BLOW_ATTACK_PROC;
//			if (_pc.getLevel() >= 75) {
//				chance = chance + (_pc.getLevel() - 74); // 1% added when level 75 or higher
//			}
//			if (chance > 30) {
//				chance = 30;
//			}
//			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= chance) {
//				_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 17223), true);
//				Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 17223), true);
//				dmg *= 1.5;
//			}
//        }
        
//        if (_weaponType == 0) { // bear hands
//            dmg = (_random.nextInt(5) + 4) / 4;
//        }

//        try {
//            dmg += WeaponAddDamage.getInstance().getWeaponAddDamage(_weaponId);
//        } catch (Exception e) {
//            System.out.println("Weapon add damage error");
//        }

        // Damage reduction of skills, cooking, etc.
        int damageReduction = 0;
//        if (_targetPc.hasSkillEffect(FOOD_KOREAN_BEEF_STEAK)
//        		|| _targetPc.hasSkillEffect(FOOD_SWIFT_STEAMED_SALMON)
//                || _targetPc.hasSkillEffect(FOOD_CLEVER_ROAST_TURKEY)) { // Renewal food
//            damageReduction += 2;
//        }
//        if (_targetPc.hasSkillEffect(FOOD_TRAINING_CHICKEN_SOUP)) {
//            damageReduction += 2;
//        }
        // Warrior Skill: Armor Guard-Gets the character's AC/10 damage reduction effect.
        if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), 237)) {
            if (_targetPc.getAC().getAc() < -10) {
                damageReduction += _targetPc.getAC().getAc() / -10;
            }
        }

//        if (_targetPc.hasSkillEffect(REDUCTION_ARMOR)) {
//            int targetPcLvl = _targetPc.getLevel();
//            if (targetPcLvl < 50) {
//                targetPcLvl = 50;
//            }
//            damageReduction += (targetPcLvl - 50) / 5 + 1;
//        }

//        if (_targetPc.hasSkillEffect(EARTH_GUARDIAN)) {
//            damageReduction += 2;
//        }

        // new damage reduction controller
        damageReduction += _targetPc.getDamageReduc();
        
        // new pvp damage reduction controller
        damageReduction += _targetPc.getPvpDamageReduction();
        
        dmg -= rumtisBlockDamage();

//        if (_targetPc.hasSkillEffect(DRAGON_SKIN)) {
//            if (_targetPc.getLevel() >= 80) {
//                damageReduction += 5 + ((_targetPc.getLevel() - 78) / 2);
//            } else {
//                damageReduction += 5;
//            }
//        }
//        if (_targetPc.hasSkillEffect(PATIENCE)) {
//            damageReduction += 2;
//        }
        if (_targetPc.hasSkillEffect(FEATHER_BUFF_A)) {
            damageReduction += 3;
        }
        if (_targetPc.hasSkillEffect(FEATHER_BUFF_B)) {
            damageReduction += 2;
        }
//        if (_targetPc.hasSkillEffect(RANKING_BUFF_2) || _targetPc.hasSkillEffect(RANKING_BUFF_3)
//                || _targetPc.hasSkillEffect(RANKING_BUFF_4)) {
//            damageReduction += 2;
//        }
//        if (_targetPc.hasSkillEffect(RANKING_BUFF_5)) { // Normal issue
//            damageReduction += 1;
//        }
        if (_targetPc.hasSkillEffect(CLAN_BUFF4)) {
            damageReduction += 1;
        }
        
        //cash shop
//        if (_targetPc.hasSkillEffect(CASH_SHOP_BUFF_1)) {
//            damageReduction += 1;
//        }
//        if (_targetPc.hasSkillEffect(CASH_SHOP_BUFF_2)) {
//            damageReduction += 2;
//        }
//        if (_targetPc.hasSkillEffect(CASH_SHOP_BUFF_3)) {
//            damageReduction += 3;
//        }
        /*
         * for (L1DollInstance doll : _targetPc.getDollList()) {//
         * マジックドールによるダメージ減少。ドルゴールレム人形 dmg -= doll.getDamageReductionByDoll(); }
         * dmg -= _targetPc.getDamageReductionByArmor(); // 防具によるダメージ減少
         */
        /*
         * _ignorereductionbyarmor =
         * armor.getItem().getIgnoreReductionByArmor(); //リダクション無視
         * _ignorereductionbyweapon =
         * weapon.getItem().getIgnoreReductionByWeapon(); //リダクション無視 int
         * ignorereduction = _ignorereductionbyarmor + _ignorereductionbyweapon;
         * int damagereductiontotal = _targetPc.getDamageReductionByArmor() +
         * damagereduction;
         *
         * if(damagereduction >= ignorereduction){ damagereduction -=
         * ignorereduction; } dmg += _ignorereductionbyarmor; dmg +=
         * _ignorereductionbyweapon; if(_pc.getInventory().checkEquipped(1)){
         * if(damagereduction > 0 && damagereduction <= 12){ damagereduction =
         * 0; }else{ damagereduction -= 12; } }
         */
        dmg -= damageReduction;

        // Damage reduction of skills, cooking, etc.
        if (_pc.hasSkillEffect(L1SkillId.RANKING_BUFF_1)) {
            dmg += 1;
        }
        if (_pc.hasSkillEffect(L1SkillId.RANKING_BUFF_2) || _pc.hasSkillEffect(L1SkillId.RANKING_BUFF_3)) {
            dmg += 2;
        }
        if (_pc.hasSkillEffect(L1SkillId.RANKING_BUFF_4)) {
            dmg += 3;
        }

        if (_pc.hasSkillEffect(L1SkillId.DESTROY)) {
            if (_pc.getWeapon().getItem().getType() == 18)
                ArmorDestory();
        }
        if (_targetPc.hasSkillEffect(ABSOLUTE_BARRIER)) {
            dmg = 0;
        }
        
        if ((_weaponType == 20 && _weaponType == GAUNTLET) && _targetPc.hasSkillEffect(MOBIUS)) {
        	dmg = 0;
        }
        if (_targetPc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        }
        if (_targetPc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }

        if (_targetPc.hasSkillEffect(PHANTASM)) {
            _targetPc.removeSkillEffect(PHANTASM);
        }
        /**if (_targetPc.hasSkillEffect(IllUSION_AVATAR)) {
            dmg += (dmg / 5);
        }**/
        
        dmg += rumtisAddDamage(); // 黒い光ピアス追加ダメージ処理

        for (L1DollInstance doll : _pc.getDollList()) { // Magic Doll Additional damage from dolls
            if (doll == null)
                continue;
            if (_weaponType != 20 && _weaponType != GAUNTLET) {
                dmg += doll.getDamageByDoll();
            }
            dmg += doll.attackPixieDamage(_pc, _targetPc);
            doll.getPixieGreg(_pc, _targetPc);
        }

        // Warrior Skill PC-PC
        // Crash: Procs 50% of the attacker's level on damage.
        if (SkillsTable.getInstance().spellCheck(_pc.getId(), 236)) {
            int chance = ThreadLocalRandom.current().nextInt(100) + 1;
            if (12 >= chance) {
                int crashDamage = _pc.getLevel() / 2;
                int gfx = 12487;
                if (SkillsTable.getInstance().spellCheck(_pc.getId(), 234)) {
                    chance = ThreadLocalRandom.current().nextInt(100) + 1;
                    if (12 >= chance) {
                    	gfx = 12489;
                        dmg += dmg + _pc.getLevel();
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), gfx));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), gfx));
                    }
                }
                dmg += crashDamage;                
                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), gfx));
                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), gfx));
            }
        }

        if (_pc.hasSkillEffect(L1SkillId.LORDS_BUFF)) {
            if (_pc.getClanRank() >= L1Clan.GUARDIAN)
                dmg += 5;
        }
        int dolldamagereduction = 0;
        for (L1DollInstance doll : _targetPc.getDollList()) {// マジックドールによるダメージ減少。ドルゴールレム人形
            dolldamagereduction = doll.getDamageReductionByDoll();
        }
        dmg -= dolldamagereduction;
        int itemamagereduction = _targetPc.getDamageReductionByArmor(); // 防具によるダメージ減少

        dmg -= itemamagereduction;
        int totaldamagereduction = 0;
        totaldamagereduction = dolldamagereduction + itemamagereduction + damageReduction;
//        if (_pc.getInventory().checkEquipped(202011)) { // Gaia's Rage
//            if (_pc.getInventory().checkEquipped(22209) || _pc.getInventory().checkEquipped(22210)) {
//                if (totaldamagereduction > 0 && totaldamagereduction <= 15) {
//                    dmg += totaldamagereduction;
//                } else {
//                    dmg += 15;
//                }
//            } else {
//                if (totaldamagereduction > 0 && totaldamagereduction <= 12) {
//                    dmg += totaldamagereduction;
//                } else {
//                    dmg += 12;
//                }
//            }
//
//        }
        
        if ((_pc.getInventory().checkEquipped(22208) || _pc.getInventory().checkEquipped(22209)
                || _pc.getInventory().checkEquipped(22210) || _pc.getInventory().checkEquipped(22211))
                && !_pc.getInventory().checkEquipped(202011)) {
            if (_pc.getInventory().checkEquipped(22208)) {
                L1ItemInstance item = _pc.getInventory().findEquippedItemId(22208);
                if (item.getEnchantLevel() == 7) {
                    dmg += 1;
                } else if (item.getEnchantLevel() == 8) {
                    dmg += 2;
                } else if (item.getEnchantLevel() >= 9) {
                    dmg += 3;
                }
            }
            if (_pc.getInventory().checkEquipped(22209)) {
                L1ItemInstance item = _pc.getInventory().findEquippedItemId(22209);
                if (item.getEnchantLevel() == 7) {
                    dmg += 1;
                } else if (item.getEnchantLevel() == 8) {
                    dmg += 2;
                } else if (item.getEnchantLevel() >= 9) {
                    dmg += 3;
                }
            }
            if (_pc.getInventory().checkEquipped(22210)) {
                L1ItemInstance item = _pc.getInventory().findEquippedItemId(22210);
                if (item.getEnchantLevel() == 7) {
                    dmg += 1;
                } else if (item.getEnchantLevel() == 8) {
                    dmg += 2;
                } else if (item.getEnchantLevel() >= 9) {
                    dmg += 3;
                }
            }
            if (_pc.getInventory().checkEquipped(22211)) {
                L1ItemInstance item = _pc.getInventory().findEquippedItemId(22211);
                if (item.getEnchantLevel() == 7) {
                    dmg += 1;
                } else if (item.getEnchantLevel() == 8) {
                    dmg += 2;
                } else if (item.getEnchantLevel() >= 9) {
                    dmg += 3;
                }
            }
            if (totaldamagereduction > 0 && totaldamagereduction <= 3) {
                dmg += totaldamagereduction;
            } else {
                dmg += 3;
            }
        }

        /** 対象の属性エンチャントによるダメージ演算 **/
        dmg += fishAttrEnchantEffect();

        /** 対象Buffによるダメージ演算 **/
        //dmg += toPcBuffDmg(dmg);

        // Additional damage by character, additional reduction, probability
        // TODO the fuck?
//        if (_calcType == PC_PC) {
//            if (_pc.getAddDamageRate() >= CommonUtil.random(100)) {
//                dmg += _pc.getAddDamage();
//            }
//            if (_targetPc.getAddReductionRate() >= CommonUtil.random(100)) {
//                dmg -= _targetPc.getAddReduction();
//            }
//        }
        // Additional damage by character, additional reduction, probability

        /** 70レベルから追加打撃+ 1 **/
        // dmg += Math.max(0, _pc.getLevel() - 70) * 1;

        if (_targetPc.hasSkillEffect(ARMOUR_BREAK)) {
            if (_weaponType != 20 && _weaponType != GAUNTLET) {
            	dmg = (dmg + 48) * 1.18;
            }
        }

        // True target
        if (_targetPc != null) {
			if (_targetPc.hasSkillEffect(L1SkillId.TRUE_TARGET)) {
				if (_pc != null) {
					if (_targetPc.tt_clanid == _pc.getClanid() || _targetPc.tt_partyid == _pc.getPartyID()) {
						if (_targetPc.tt_level >= 90) {
							dmg += dmg * 0.08;
						} else if (_targetPc.tt_level >= 85) {
							dmg += dmg * 0.07;
						} else if (_targetPc.tt_level >= 80) {
							dmg += dmg * 0.06;
						} else if (_targetPc.tt_level >= 75) {
							dmg += dmg * 0.05;
						} else if (_targetPc.tt_level >= 60) {
							dmg += dmg * 0.04;
						} else if (_targetPc.tt_level >= 45) {
							dmg += dmg * 0.03;
						} else if (_targetPc.tt_level >= 30) {
							dmg += dmg * 0.02;
						} else if (_targetPc.tt_level >= 15) {
							dmg += dmg * 0.01;
						}
					}
				}
			}
		}

        /** New clan attack tremendous **/
        if (_calcType == PC_PC) {
            int castle_id = L1CastleLocation.getCastleIdByArea(_pc);
            boolean isAliveBoss = BossAlive.getInstance().isBossAlive(_targetPc.getMapId());
            if (castle_id == 0 && !isAliveBoss) {
                if (_pc.getClanid() == Config.NEW_CLAN || _targetPc.getClanid() == Config.NEW_CLAN) {
                    if (Config.NEW_CLAN_PROTECTION_PROCESS) {
                        _isHit = false;
                        _pc.sendPackets(new S_SystemMessage("The new protection clan is not attacking each other."));
                        _targetPc.sendPackets(new S_SystemMessage("The new protective clans are not attacking each other."));
                    } else {
                        dmg /= 2;
                        _pc.sendPackets(new S_SystemMessage("The new protection clan takes only 50% of the damage."));
                        _targetPc.sendPackets(new S_SystemMessage("The new protective clan only takes 50% damage."));
                    }
                }
            }
        }
        /** New clan attack tremendously **/

        if (_pc.hasSkillEffect(BURNING_SLASH)) {
            if (_weaponType != 20) {
                dmg += 30;
                _pc.sendPackets(new S_SkillSound(_targetPc.getId(), 6591));
                _pc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 6591));
                _pc.removeSkillEffect(BURNING_SLASH);
            }
        }

        // traitors shield change damage reduction
        if (_targetPc.getInventory().checkEquipped(22263)) {
            int chance = _random.nextInt(100) + 1;
            L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22263);
            int enchant = item.getEnchantLevel();
        	if (chance <= (enchant * 2)) {
                dmg -= 50;
                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 6320));
                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 6320));
            }
        }
        if (_targetPc.getInventory().checkEquipped(222355)) {// 神聖なエルヴンシールド
            int chance = _random.nextInt(100) + 1;
            L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(222355);
            if (chance <= item.getEnchantLevel()) {
                dmg -= 20;
                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 14543));
                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 14543));
            }
        }
        // Protection of Lindvior
        int chance6 = _random.nextInt(100) + 1;
        if (dmg > 25) {
            if (_target != _targetNpc) {
                if (_targetPc.getInventory().checkEquipped(22204)) {// リンドビオル ストーム プレート メイル
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22204);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 10);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                } else if (_targetPc.getInventory().checkEquipped(22205)) {// リンドビオル ストーム スケイル メイル
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22205);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 15);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                } else if (_targetPc.getInventory().checkEquipped(22206)) {// リンドビオル ストーム レザー アーマー
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22206);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 20);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                } else if (_targetPc.getInventory().checkEquipped(22207)) {// リンドビオル ストーム ローブ
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22207);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 20);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                }
            }
        }

        // Blessing of Fafurion
        int fafBlessingProcChance = _random.nextInt(100) + 1;
        if (_target != null) {
            if (_targetPc.getInventory().checkEquipped(22200) || // Fafurion Plate Mail
                    _targetPc.getInventory().checkEquipped(22201) || // Fafurion Scale Mail
                    _targetPc.getInventory().checkEquipped(22202) || // Fafurion Leather Armour
                    _targetPc.getInventory().checkEquipped(22203)) { // Fafurion Robe
                if (fafBlessingProcChance <= 6) {
                	int dmg2 = 0;
                    int plus = 0;
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22200);
                    L1ItemInstance item1 = _targetPc.getInventory().findEquippedItemId(22201);
                    L1ItemInstance item2 = _targetPc.getInventory().findEquippedItemId(22202);
                    L1ItemInstance item3 = _targetPc.getInventory().findEquippedItemId(22203);
                    if (item.getEnchantLevel() >= 7 && item.getEnchantLevel() <= 9) {
                        plus = item.getEnchantLevel() - 6;
                    } else if (item1.getEnchantLevel() >= 7 && item1.getEnchantLevel() <= 9) {
                        plus = item.getEnchantLevel() - 6;
                    } else if (item2.getEnchantLevel() >= 7 && item2.getEnchantLevel() <= 9) {
                        plus = item.getEnchantLevel() - 6;
                    } else if (item3.getEnchantLevel() >= 7 && item3.getEnchantLevel() <= 9) {
                        plus = item.getEnchantLevel() - 6;
                    } else if (item.getEnchantLevel() > 9
                    		|| item1.getEnchantLevel() > 9
                    		|| item2.getEnchantLevel() > 9
                            || item3.getEnchantLevel() > 9) {
                        plus = 3;
                    }
                    if (_targetPc.hasSkillEffect(L1SkillId.POLLUTE_WATER)) {
                        dmg2 += (40 + _random.nextInt(15) + (plus * 10)) / 2; // Half of the flute water case // the original random number 30
                    }
                    if (_targetPc.hasSkillEffect(L1SkillId.WATER_LIFE)) {
                        dmg2 += (40 + _random.nextInt(15) + (plus * 10)) * 2; // If water life is doubled // the original random number is 30
                    }
                    dmg2 += 40 + _random.nextInt(15) + (plus * 10); // Recovery rate = Basic 50 recovery + Random (1 to 30) // Original random number 30
                    _targetPc.setCurrentHp(_targetPc.getCurrentHp() + dmg2);
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2187));
                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2187));
                }
            }
        }

        // Divine Elven Plate Mail
        int chance66 = _random.nextInt(100) + 1;
        if (_target != null) {
            int dmg2 = 0;
            int plus = 0;
            if (_targetPc.getInventory().checkEquipped(222351)) {
                if (chance66 <= 5) { // 元5である
                    if (_targetPc.hasSkillEffect(L1SkillId.POLLUTE_WATER)) {
                        dmg2 += (25 + _random.nextInt(15) + (plus * 10)) / 2; //
                    }
                    if (_targetPc.hasSkillEffect(L1SkillId.WATER_LIFE)) {
                        dmg2 += (25 + _random.nextInt(15) + (plus * 10)) * 2; //
                    }
                    dmg2 += 25 + _random.nextInt(15) + (plus * 10); //
                    _targetPc.setCurrentHp(_targetPc.getCurrentHp() + dmg2);
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 14543));
                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 14543));
                }
            }
        }

        /** Damage reduction correction by AC**/
        if (dmg <= 0) {
            _isHit = false;
        }
        /** Risks from Abyss Points */

        if (_targetPc.getPeerage() == 1) {
            dmg -= 0.5;
        } else if (_targetPc.getPeerage() == 2) {
            dmg -= 1;
        } else if (_targetPc.getPeerage() == 3) {
            dmg -= 1.5;
        } else if (_targetPc.getPeerage() == 4) {
            dmg -= 2;
        } else if (_targetPc.getPeerage() == 5) {
            dmg -= 2.5;
        } else if (_targetPc.getPeerage() == 6) {
            dmg -= 3;
        } else if (_targetPc.getPeerage() == 7) {
            dmg -= 3.5;
        } else if (_targetPc.getPeerage() == 8) {
            dmg -= 4;
        } else if (_targetPc.getPeerage() == 9) {
            dmg -= 4.5;
        } else if (_targetPc.getPeerage() == 10) {
            dmg -= 5;
        } else if (_targetPc.getPeerage() == 11) {
            dmg -= 5.5;
        } else if (_targetPc.getPeerage() == 12) {
            dmg -= 6;
        } else if (_targetPc.getPeerage() == 13) {
            dmg -= 6.5;
        } else if (_targetPc.getPeerage() == 14) {
            dmg -= 7;
        } else if (_targetPc.getPeerage() == 15) {
            dmg -= 7.5;
        } else if (_targetPc.getPeerage() == 16) {
            dmg -= 8;
        } else if (_targetPc.getPeerage() == 17) {
            dmg -= 8.5;
        } else if (_targetPc.getPeerage() == 18) {
            dmg -= 9;
        }
        
        if (_targetPc.hasSkillEffect(IMMUNE_TO_HARM)) {
        	if (_targetPc.isWizard()) {
        		dmg -= (dmg * Config.RATE_IMMUNE_HARM);
        	} else {
        		double additionalImmune = 0;
        		if (_targetPc.getLevel() >= 86) {
        			additionalImmune = 0.35;
        		} else if (_targetPc.getLevel() >= 84) {
        			additionalImmune = 0.30;
        		} else if (_targetPc.getLevel() >= 82) {
        			additionalImmune = 0.25;
        		} else if (_targetPc.getLevel() >= 80) {
        			additionalImmune = 0.20;
        		} else if (_targetPc.getLevel() >= 78) {
        			additionalImmune = 0.15;
        		} else if (_targetPc.getLevel() >= 76) {
        			additionalImmune = 0.10;
        		} else if (_targetPc.getLevel() >= 74) {
        			additionalImmune = 0.05;
        		}
        		dmg -= (dmg * (Config.RATE_IMMUNE_HARM_OTHERS + additionalImmune));
                }
            }
        //System.out.println("Final DMG PvP: " + dmg);
        return (int) dmg;
    }

    // Damage calculation from PC to NPC
    private int calcPcNpcDamage() {
        if (_targetNpc == null || _pc == null) {
            _isHit = false;
            _drainHp = 0;
            return 0;
        }

        int maxWeaponDamage = 0;
		int weaponDamageOut = 0;
		int weaponTotalBonus = _weaponEnchant + _weaponAddDmg;

        if (_targetNpc.getNpcTemplate().get_size().equalsIgnoreCase("small") && _weaponSmall > 0) {
        	maxWeaponDamage = _weaponSmall;
        } else if (_targetNpc.getNpcTemplate().get_size().equalsIgnoreCase("large") && _weaponLarge > 0) {
        	maxWeaponDamage = _weaponLarge;
        }
        
        // over enchant buffs 2018
        if (_weaponType != 0) {
            if (_weaponId != 66) {
                switch (weapon.getEnchantLevel()) {
                case 10:
                	weaponTotalBonus += 1;
                    break;
                case 11:
                	weaponTotalBonus += 2;
                    break;
                case 12:
                	weaponTotalBonus += 3;
                    break;
                case 13:
                	weaponTotalBonus += 4;
                    break;
                case 14:
                	weaponTotalBonus += 5;
                    break;
                case 15:
                	weaponTotalBonus += 6;
                    break;
                default:
                	if (weapon.getEnchantLevel() >= 16) {
                		weaponTotalBonus += 6; // Handle cases 16 and above
                    }
                    break;
                }
            }
        }
        
        if (_weaponType != BOW && _weaponType != GAUNTLET) { // PvE not bow/gauntlet
        	weaponTotalBonus += _statsBonusDamage + _pc.getDmgup();
        } else {
        	weaponTotalBonus += _statsBonusDamage + _pc.getBowDmgup();
        }

        if (_weaponType == 58) { // Claw PvE
            int clawProcChance = _random.nextInt(100) + 1;
            if (clawProcChance <= _weaponDoubleDmgChance) {
            	weaponDamageOut = maxWeaponDamage + weaponTotalBonus;
                _pc.sendPackets(new S_SkillSound(_pc.getId(), 3671));
                _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 3671));
            } else {
            	weaponDamageOut = (_random.nextInt(maxWeaponDamage)) + weaponTotalBonus;
            }
        } else if (_weaponType == 0) { // Bare hands
            weaponDamageOut = 1;
        } else {
            weaponDamageOut = (_random.nextInt(maxWeaponDamage)) + weaponTotalBonus;
        }
        
        if (_pc.hasSkillEffect(SOUL_OF_FLAME)) {
            if (_weaponType != 20 && _weaponType != GAUNTLET) {
                weaponDamageOut = maxWeaponDamage + weaponTotalBonus;
            }
        }
        if (_weaponType != 0) {
            if (_weaponType != BOW && _weaponType != GAUNTLET && _weaponType != 17) { // melee distance
                int criticalFromSTR = CalcStat.calcChanceToCritSTR(_pc.getAbility().getBaseStr()) + _pc.getDmgCritical();
                int chanceToCritical = _random.nextInt(100) + 1;
                
                // Strike of Valakas
                if (_pc.getInventory().checkEquipped(22208)
                		|| _pc.getInventory().checkEquipped(22209)
                        || _pc.getInventory().checkEquipped(22210)
                        || _pc.getInventory().checkEquipped(22211)) { // needs increase crit at +7 +8 +9
                    int strikeOfValakasChance = _random.nextInt(100) + 1;
                    if (strikeOfValakasChance <= 3) {
                        weaponDamageOut = maxWeaponDamage + weaponTotalBonus;
                        S_UseAttackSkill packet = new S_UseAttackSkill(_target, _target.getId(), 15841, _targetX, _targetY, ActionCodes.ACTION_Attack, false);
                        _pc.sendPackets(packet);
                        Broadcaster.broadcastPacket(_pc, packet);
                    }
                }
                
                if (chanceToCritical <= criticalFromSTR) {
                	weaponDamageOut = maxWeaponDamage + weaponTotalBonus;
                    _isCritical = true;
                }
            } else {
                int bowCriticalBonus = CalcStat.calcBowCritical(_pc.getAbility().getBaseDex()) + _pc.getBowDmgCritical();
                int bowChanceToCritical = _random.nextInt(100) + 1;
                
                if (_pc.hasSkillEffect(EAGLE_EYE)) {
                	bowCriticalBonus += 2;
                }

                if (bowChanceToCritical <= bowCriticalBonus) {
					weaponDamageOut = maxWeaponDamage + weaponTotalBonus;
                    _isCritical = true;
                }
            }
        }
        //int weaponCalcDamage = weaponDamage + _weaponEnchant + _weaponAddDmg;

        //weaponTotalDamage += calcMaterialBlessDmg(); // Blessing damage bonus

//        if (_pc.hasSkillEffect(QUAKE) && _weaponType != 20 && _weaponType != GAUNTLET) {
//        	if ((_random.nextInt(100) + 1) <= 12) {
//        		weaponTotalDamage *= 1.5;
//            	System.out.println("Quake: " + weaponTotalDamage);
//            }
//        }

        if (_weaponId == 203018) { // Roar Edoryu
            _weaponDoubleDmgChance += _pc.getWeapon().getEnchantLevel();
        }

        if (_weaponType == EDORYU && (_random.nextInt(100) + 1) <= (_weaponDoubleDmgChance - weapon.get_durability())) { // standard edo crit
        	weaponDamageOut *= 2; // pve
            _pc.sendPackets(new S_SkillSound(_pc.getId(), 3398));
            _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 3398));
        }
        
        if (_pc.hasSkillEffect(DOUBLE_BREAK) && (_weaponType == 54 || _weaponType == 58)) { // Double brake probability
        	//System.out.println("player has double break and edo or claw");
			int rnd = 25;
			switch (_pc.getLevel()) {
			case 98:
				rnd += 5;
				break;
			case 96:
				rnd += 4;
				break;
			case 94:
				rnd += 3;
				break;
			case 92:
				rnd += 2;
				break;
			case 90:
				rnd += 1;
				break;
			default:
				rnd = 25;
				break;
			}
			
			if ((_random.nextInt(100) + 1) <= rnd) { // PvE
				weaponDamageOut *= 2;
				}
			}

        double dmg = weaponDamageOut;

        if (_weaponType2 == 17) { // Kiringku PvE
        	int dmgFromInt = _pc.getAbility().getTotalInt();
        	int dmgFromSP = _pc.getAbility().getSp();
        	//int dmgMaxRoll = dmgFromInt + dmgFromSP;
        	int kiringkuRandom = (ThreadLocalRandom.current().nextInt(dmgFromInt) + 4);
        	int randomChance = (ThreadLocalRandom.current().nextInt(100) + 1);
        	
        	dmg += kiringkuRandom + dmgFromSP;
        	
        	int magicCritical = CalcStat.calcINTMagicCritical(_pc.getAbility().getTotalInt()) + _pc.getMagicBonus();
        	if (randomChance <= magicCritical) {
				dmg *= 1.25;
				_isCritical = true;
//				System.out.println("magicCritical " + magicCritical);
//				System.out.println("kiringku crit " + dmg);
			} else {
				dmg = calcMrDefense(_target.getResistance().getEffectedMrBySkill(), dmg);
			}
        }
        
        dmg += fishAttrEnchantEffect(); // Attribute damage

        if (_weaponType == 20) { // bow
            if (_arrow != null) {
                int add_dmg = 0;
                if (_targetNpc.getNpcTemplate().get_size().equalsIgnoreCase("large")) {
                    add_dmg = _arrow.getItem().getDmgLarge();
                } else {
                    add_dmg = _arrow.getItem().getDmgSmall();
                }
                if (add_dmg == 0) {
                    add_dmg = 1;
                }
                if (_targetNpc.getNpcTemplate().is_hard() && weapon.getItem().get_penetration() != 1) {
                    add_dmg /= 2;
                }
                dmg = dmg + attrArrow(_arrow, _targetNpc);
            } else if (_weaponId == 190 || _weaponId == 10000 || _weaponId == 202011) { // Saiha's bow
                dmg = dmg + _random.nextInt(15) + 4;
            }
        } else if (_weaponType == GAUNTLET) {
            int add_dmg = 0;
            if (_targetNpc.getNpcTemplate().get_size().equalsIgnoreCase("large")) {
                add_dmg = _sting.getItem().getDmgLarge();
            } else {
                add_dmg = _sting.getItem().getDmgSmall();
            }
            if (add_dmg == 0) {
                add_dmg = 1;
            }
            dmg = dmg + _random.nextInt(add_dmg) + attrArrow(_arrow, _targetNpc);
        }

        // arrows
        if (_arrow != null) {
        	if (_arrow.getItem().getItemId() == 820014
        			|| _arrow.getItem().getItemId() == 820015
        			|| _arrow.getItem().getItemId() == 820016
        			|| _arrow.getItem().getItemId() == 820017
        			|| _arrow.getItem().getItemId() == 40744) { // hunters silver arrow
        		dmg += 3;
        	} else if (_arrow.getItem().getItemId() == 40743) { // hunters arrow
        		dmg += 1;
        	}
        }
        
        /** Red Knight's Great Sword Renewal **/
        if (_pc.getInventory().checkEquipped(202002) || _pc.getInventory().checkEquipped(203002)
                || _pc.getInventory().checkEquipped(1136) || _pc.getInventory().checkEquipped(1137)) {
            if (_pc.getLawful() < -32760) {
                dmg += 8;
            }
            if (_pc.getLawful() >= -32760 && _pc.getLawful() < -25000) {
                dmg += 6;
            }
            if (_pc.getLawful() >= -25000 && _pc.getLawful() < -15000) {
                dmg += 4;
            }
            if (_pc.getLawful() >= -15000 && _pc.getLawful() < 0) {
                dmg += 2;
            }
        }
        
        // bonus damage from lawful/chaotic
        dmg += _pc.getBapodmg();
        //System.out.println("lawful: " + _pc.getBapodmg());
        
        // Burning spirits, elemental fire, brave mental, BLOW_ATTACK 1.5 times skill effect and ivy part
        int skillCriticalBuff = _random.nextInt(100) + 1;
        if (_weaponType != 20 && _weaponType != GAUNTLET && _weaponType2 != 17) { // bow gauntlet kiringku
            if (_pc.hasSkillEffect(ELEMENTAL_FIRE) || _pc.hasSkillEffect(BRAVE_MENTAL) || _pc.hasSkillEffect(QUAKE)) {
            	if (skillCriticalBuff <= 12) {
            		if (_calcType == PC_PC) {
    					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 7727), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 7727), true);
    				} else if (_calcType == PC_NPC) {
    					_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 7727), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 7727), true);
    				}
                	dmg *= 1.5;
                }
            } else if (_pc.hasSkillEffect(BURNING_SPIRIT)) { // PVE
            	if (skillCriticalBuff <= 33) {
            		if (_calcType == PC_PC) {
						_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 6532), true);
						Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 6532), true);
					} else if (_calcType == PC_NPC) {
						_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 6532), true);
						Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 6532), true);
					}
            		dmg *= 1.5;
            		//System.out.println("BURNING_SPIRIT ran: " + dmg);
            	}
            } else if (_pc.hasSkillEffect(BLOW_ATTACK)) {
            	int blowAttackChance = Config.BLOW_ATTACK_PROC; // current 5%
    			if (_pc.getLevel() >= 75) {
    				blowAttackChance = blowAttackChance + (_pc.getLevel() - 74); // 1% added when level 75 or higher
    			}
    			if (blowAttackChance > 20) {
    				blowAttackChance = 20;
    			}
    			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= blowAttackChance) {
    				if (_calcType == PC_PC) {
    					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 17223), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 17223), true);
    				} else if (_calcType == PC_NPC) {
    					_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 17223), true);
    					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 17223), true);
    				}
    				dmg *= 1.5;
    			}
            }
        }
        
        if (_pc.hasSkillEffect(CYCLONE) && (_weaponType == 20 || _weaponType == GAUNTLET || _weaponType == 17)) {
			int cycloneProcChance = 5;
			if (_pc.getLevel() > 75) {
				cycloneProcChance = cycloneProcChance + (_pc.getLevel() - 75); // 1% added at level 75 and above
			}
	
			if (cycloneProcChance > 20) { // maximum probability
				cycloneProcChance = 20;
			}
	
			if ((ThreadLocalRandom.current().nextInt(100) + 1) <= cycloneProcChance) {
				if (_calcType == PC_PC) {
					_pc.sendPackets(new S_SkillSound(_targetPc.getId(), 17557), true);
					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetPc.getId(), 17557), true);
				} else if (_calcType == PC_NPC) {
					_pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 17557), true);
					Broadcaster.broadcastPacket(_pc, new S_SkillSound(_targetNpc.getId(), 17557), true);
				}
				dmg *= 1.5;
				//System.out.println("CyclonePvE " + dmg);
			}
        }

        switch (_weaponId) {
        //kiringku
        case 283: // Valakas Kiringku
            dmg += L1WeaponSkill.valakasWeaponProc(_pc, _target, 10405, _weaponEnchant);
            break;
		case 1112: // Hidden Demon Kiringku
			dmg += evilTrick(_pc, _target, 8152, _weaponEnchant);
			break;
		case 1120: // Cold sensitivity key link
            dmg += L1WeaponSkill.iceColdKiringku(_pc, _target, 6553, _weaponEnchant);
            break;
        case 1135: // Resonance key link
            dmg += L1WeaponSkill.Kiringku_Resonance(_pc, _target, 5201, _weaponEnchant);
			break;
        case 202012: // Hyperion's despair
            dmg += L1WeaponSkill.hyperionsDespair(_pc, _target, 12248, _weaponEnchant);
			break;

		// bow

		// arnold weapons
        case 307:
        case 308:
        case 309:
        case 310:
        case 311:
        case 313:
        case 314:
            dmg = L1WeaponSkill.blazeShock(_pc, _targetNpc, _weaponEnchant);
            break;
        case 291: // demon king kiringku
        case 292: // demon king sword
        case 1010:
        case 1011:
        case 1012:
        case 1013:
        case 1014:
        case 1015: // Demon King Axe
            L1WeaponSkill.getDiseaseWeapon(_pc, _targetNpc, _weaponId);
            break;
        case 12:
			ruinGreatSword(dmg);
			dmg += L1WeaponSkill.LordSword(_pc, _targetNpc, 4842, _weaponEnchant);
            break;// 風の刃短剣
        case 203020: // 生命の短剣
        case 601: // 破滅のグレートソード
            ruinGreatSword(dmg);
            break;
         // edoryu
        case 76: // Edoryu of Ronde
        	dmg += L1WeaponSkill.revengeProc(_pc, _targetNpc, 10145, _weaponEnchant);
        	break;
        case 86: // Edoryu of Red Shadow
			L1WeaponSkill.redShadowEdoryu(_pc, _targetNpc);
            break;
		case 204: // crimson crossbow
        case 100204: // crimson shaft crossbow
        	// TODO does nothing? no bonus dmg
            L1WeaponSkill.redShadowEdoryu(_pc, _targetNpc);
            break;
//        case 1115: // Mysterious Sword
//        case 1117: // Mysterious Claw
//            dmg += reverseEvil(_pc, _target, 8981, _weaponEnchant);
//            break;
        case 1116: // 神妙杖
        case 1118: // Mysterious longbow
        case 202011: // がよの激怒
            dmg += getEbMP1(_pc, _target, 8981, _weaponEnchant);
            break;
        case 1109: // Hidden Demon Claw
        case 1113: // Hidden Demon Sword
        case 1114: // Hidden Demon Sword 2hs
        case 1115: // Mysterious Sword
        case 1117: // Mysterious Claw
        case 203011: // Hidden Demon Axe
            dmg += reverseEvil(_pc, _target, 8150, _weaponEnchant);
            break;
        case 1110: // Hidden Demon Staff
		case 1111: // Hidden Demon Bow
            dmg += evilTrick(_pc, _target, 8152, _weaponEnchant);
            break;
        case 1108: // 魔族チェーン
            dmg += reverseEvil(_pc, _target, 8150, _weaponEnchant);
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 1119: // 極限のチェーンソード
            dmg += L1WeaponSkill.iceEruption(_pc, _target, 3685, _weaponEnchant);
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 1123: // Bloodserker
        	L1WeaponSkill.ChainSword(_pc,_target);
        	bloodSucker(dmg, _weaponEnchant);
        	break;
        case 202013:
            L1WeaponSkill.ChainSword(_pc,_target);
			dmg += L1WeaponSkill.LordSword(_pc, _target, 4842, _weaponEnchant);
            break;
        case 500:// デストラクタのチェーンソード
        case 501:// 破滅者のチェーンソード
        case 1104:// エルモアチェーンソード
        case 1132:// ベビーテルランチェーンソード
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 312:
            dmg = L1WeaponSkill.ChainSword_BlazeShock(_pc, _targetNpc, _weaponEnchant);
            break;
        case 203017:
            L1WeaponSkill.ChainSword_Destroyer(_pc);
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.annihilate(_pc, _target, 4077, _weaponEnchant);
            break;
        case 203006:// Typhoon Axe
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.hellStormProc(_pc, _target, 7977, _weaponEnchant);
            break;
        case 1136: // Nightmare longbow
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.nightmareProc(_pc, _target, 14339, _weaponEnchant);
            break;
        case 203025: // Jin Fighting Avian Greatsword
        case 203026: // Jin Fighting Avian Greatsword B
            if (weapon.getEnchantLevel() >= 10)
                dmg += L1WeaponSkill.Jinsa(_pc, _target, 8032, _weaponEnchant);
            break;
        case 202001: // Chainsword of Illusion
            dmg += L1WeaponSkill.ChainSword_BlazeShock(_pc, _target, _weaponEnchant);
            L1WeaponSkill.ChainSword(_pc,_target);
            break;
        case 1124: // 破壊の二刀流
        case 1125: // 破壊のクロウ
        case 11125:// 祝福破壊の二刀流
            dmg += L1WeaponSkill.DestructionDualBlade_Crow(_pc, _target, 9359, _weaponEnchant);
            break;
        case 600: // Thunder God Sword
            dmg += L1WeaponSkill.electricShockProc(_pc, _target, 3940, _weaponEnchant);
            break;
        case 604: // 酷寒のウィンドウ
            dmg += L1WeaponSkill.ExColdWind(_pc, _target, 3704, _weaponEnchant);
            break;
        case 605: // 狂風の斧
        case 203015: // 疾風の斧
            dmg += L1WeaponSkill.InsanityWindAx(_pc, _target, 5524, _weaponEnchant);
            break;
        case 191: // Angel Slayer
            dmg += L1WeaponSkill.AngelSlayer(_pc, _target, 9361, _weaponEnchant);
            break;
        case 294:
		case 61:
		case 202014:
            dmg += L1WeaponSkill.LordSword(_pc, _target, 4842, _weaponEnchant);
            break;
        case 58: // Death Knight Flame Blade
            dmg += L1WeaponSkill.deathKnightFireSwordPvE(_pc, _target, _weaponEnchant, 7300);
            break;
        case 54:
            dmg += L1WeaponSkill.kurtzSword(_pc, _target, _weaponEnchant, 10405);
            break;
        case 124:
            dmg += L1WeaponSkill.staffOfBaphomet(_pc, _target, _weaponEnchant, 129);
            break;
        case 202003: // Zelos Wand
            dmg += L1WeaponSkill.zerosStaff(_pc, _target, _weaponEnchant, 11760);
            break;
        case 134: // Crystalized Staff
            dmg += L1WeaponSkill.lightningStrikePvE(_pc, _target, _weaponEnchant, 10405);
            break;
        case 603: // Angel's wand
            L1WeaponSkill.AngelStaff(_pc, _target, _weaponEnchant);
            break;
//        case 450004: // Fonos Bume Smache
//        	dmg += L1WeaponSkill.fonosBumeSmache(_pc, _target, _weaponEnchant, 762);
//        	break;
        default:
            dmg += L1WeaponSkill.getWeaponSkillDamage(_pc, _target, _weaponId);
            break;
        }

        if (_weaponType == 0) { // bare hands
            //dmg = (_random.nextInt(5) + 4) / 4;
            dmg = 1;
        }

//        try {
//            dmg += WeaponAddDamage.getInstance().getWeaponAddDamage(_weaponId);
//        } catch (Exception e) {
//            System.out.println("Weapon Add damage Error");
//        }

        dmg += rumtisAddDamage(); // Black light piercing additional damage treatment

        if (_pc.hasSkillEffect(BURNING_SLASH)) {
            if (_weaponType != 20 && _weaponType != GAUNTLET) {
                dmg += 20;
                _pc.sendPackets(new S_SkillSound(_targetNpc.getId(), 6591));
                _pc.broadcastPacket(new S_SkillSound(_targetNpc.getId(), 6591));
                _pc.removeSkillEffect(BURNING_SLASH);
            }
        }
        for (L1DollInstance doll : _pc.getDollList()) {// Magic Doll Additional damage from dolls
            if (doll == null)
                continue;
            if (_weaponType != 20 && _weaponType != GAUNTLET) {
                dmg += doll.getDamageByDoll();
            }
            dmg += doll.attackPixieDamage(_pc, _targetNpc);
            doll.getPixieGreg(_pc, _targetNpc);
        }

        // Warrior Skill PC-NPC
        // Crash: Reflects about 50% of the attacker's level on damage.
        if (SkillsTable.getInstance().spellCheck(_pc.getId(), 236)) {
            int chance = ThreadLocalRandom.current().nextInt(100) + 1;
            if (8 >= chance) {
                //
                int crashDamage = _pc.getLevel() / 2;
                if (SkillsTable.getInstance().spellCheck(_pc.getId(), 234)) {
                    chance = ThreadLocalRandom.current().nextInt(100) + 1;
                    if (4 >= chance) {
                        dmg += dmg + _pc.getLevel();
                        _targetNpc.broadcastPacket(new S_SkillSound(_targetNpc.getId(), 12489));
                    }
                }
                dmg += crashDamage;
                _targetNpc.broadcastPacket(new S_SkillSound(_targetNpc.getId(), 12487));
            }
        }
        
        // Blessing damage bonus
        dmg += calcMaterialBlessDmg(); 
        
        //dmg -= calcNpcDamageReduction();

        boolean isNowWar = false;
        int castleId = L1CastleLocation.getCastleIdByArea(_targetNpc);
        if (castleId > 0) {
            isNowWar = WarTimeController.getInstance().isNowWar(castleId);
        }
        if (!isNowWar) {
            if (_targetNpc instanceof L1PetInstance) {
                dmg /= 2;
            }
//            if (_targetNpc instanceof L1SummonInstance) {
//                L1SummonInstance summon = (L1SummonInstance) _targetNpc;
//                if (summon.isExsistMaster()) {
//                    dmg /= 2;
//                }
//            }
        }

        if (_targetNpc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(PHANTASM)) {
            _targetNpc.removeSkillEffect(PHANTASM);
        }
        if (dmg <= 0) {
            _isHit = false;
        }
        
        if (_pc._showPvEDamage) {
        	_pc.sendPackets(new S_SystemMessage("\\fR Damage [" + dmg + "]"));
        }
        //System.out.println("Final Damage PvE: " + dmg);
        return (int) dmg;
    }

    // ●●●● Damage calculation from NPC to player ●●●●
    private int calcNpcPcDamage() {
        if (_npc == null || _targetPc == null)
            return 0;

        int lvl = _npc.getLevel();
        double dmg = 0D;
        if (_targetPc instanceof L1RobotInstance) {
            dmg = 20;

        }
        
//        if (lvl < 10) // mob level less than 10
//            dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr();
//        else if (lvl >= 10 && lvl < 20)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr();
//		else if (lvl >= 20 && lvl < 30)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr();
//		else if (lvl >= 30 && lvl < 40)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 2;
//		else if (lvl >= 40 && lvl < 50)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 4;
//		else if (lvl >= 50 && lvl < 60)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 6;
//		else if (lvl >= 60 && lvl < 70)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 8;
//		else if (lvl >= 70 && lvl < 80)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 10;
//		else if (lvl >= 80 && lvl < 87)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 20;
//		else if (lvl >= 87)
//			dmg = _random.nextInt(lvl / 2) + _npc.getAbility().getTotalStr() + 40;
//		else if (lvl >= 94)
//			dmg = _random.nextInt(lvl / 2) + (_npc.getAbility().getTotalStr() * 2) + 100;
        
        if (lvl < 10) // mob level less than 10
            dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
        else if (lvl >= 10 && lvl < 20)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 20 && lvl < 30)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 30 && lvl < 40)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 40 && lvl < 50)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 50 && lvl < 60)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 60 && lvl < 70)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 70 && lvl < 80)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 80 && lvl < 87)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 87)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + _npc.getAbility().getTotalStr();
		else if (lvl >= 94)
			dmg = _random.nextInt(_npc.getAbility().getTotalStr() / 2) + (_npc.getAbility().getTotalStr() * 2);

        if (_npc instanceof L1PetInstance) {
            dmg += (lvl / 12); // +1 dmg every 12 levels
            dmg += ((L1PetInstance) _npc).getDamageByWeapon();
        }
        dmg += _npc.getDmgup();

        if (isUndeadDamage()) {
        	dmg *= 1.10;
        }
        if (_npc.getMapId() == 1700 /* || _npc.getMapId()== ??? */) { // If it's a forgotten island
            dmg *= 1.45; // 40% more damage on forgotten island?
        }
        /* *//** 特定のマップのモンスターセゲ **//*
                                    * if (_npc.getMapId() == 30) { dmg = (dmg *
                                    * getLeverage()) / 0; //数字を上げるほど。するとれる }
                                    */
        /** 全モンスターセゲ **/
        // dmg = dmg * getLeverage() / 13;//<モンスターの物理ダメージ上げれば弱まる。
        dmg = dmg * getLeverage() / Config.NPC_DMG; // npcExternalization of physical damage
        dmg -= (calcPcDefense() / 2);

        if (_npc.isWeaponBreaked()) { // NPC Is on weapon break.
            dmg *= 0.5;
        }

        for (L1DollInstance doll : _targetPc.getDollList()) { // Damage reduction by magic doll. Stone golem
            dmg -= doll.getDamageReductionByDoll();
        }

        dmg -= _targetPc.getDamageReductionByArmor(); // Reduced damage from armor

        /** Damage calculation by target Buff
        // dmg + = toPcBuffDmg (dmg);
        // Damage reduction for skills, cooking, etc**/
        int damageReduction = 0;
//        if (_targetPc.hasSkillEffect(FOOD_KOREAN_BEEF_STEAK)
//        		|| _targetPc.hasSkillEffect(FOOD_SWIFT_STEAMED_SALMON)
//                || _targetPc.hasSkillEffect(FOOD_CLEVER_ROAST_TURKEY)) { // Renewal food
//            damageReduction += 2;
//        }
//        if (_targetPc.hasSkillEffect(FOOD_TRAINING_CHICKEN_SOUP)) {
//            damageReduction += 2;
//        }
        // Warrior Skill: Armor Guard-Gets the character's AC / 10 damage reduction effect.
        if (SkillsTable.getInstance().spellCheck(_targetPc.getId(), 237)) {
            if (_targetPc.getAC().getAc() < -10) {
                damageReduction += _targetPc.getAC().getAc() / -10;
            }
        }
        
        // new damage reduction controller
        damageReduction += _targetPc.getDamageReduc();
        
//        if (_targetPc.hasSkillEffect(MAJESTY)) {
//        	int targetPcLvl = _targetPc.getLevel();
//			if (targetPcLvl < 80) {
//				targetPcLvl = 80;
//			}
//			dmg -= (targetPcLvl - 80) + 3;
//		}

        dmg -= rumtisBlockDamage();

//        if (_targetPc.hasSkillEffect(DRAGON_SKIN)) {
//            if (_targetPc.getLevel() >= 80) {
//                damageReduction += 5 + ((_targetPc.getLevel() - 78) / 2);
//            } else {
//                damageReduction += 5;
//            }
//        }

        if (_targetPc.hasSkillEffect(FEATHER_BUFF_A)) {
            damageReduction += 3;
        }
        if (_targetPc.hasSkillEffect(FEATHER_BUFF_B)) {
            damageReduction += 2;
        }
        if (_targetPc.hasSkillEffect(CLAN_BUFF4)) {
            damageReduction += 1;
        }
        dmg -= damageReduction;

        if (_targetPc.hasSkillEffect(IMMUNE_TO_HARM)) {
        	if (_targetPc.isWizard()) {
        		dmg -= (dmg * Config.RATE_IMMUNE_HARM);
        	} else {
        		double additionalImmune = 0;
        		if (_targetPc.getLevel() >= 86) {
        			additionalImmune = 0.35;
        		} else if (_targetPc.getLevel() >= 84) {
        			additionalImmune = 0.30;
        		} else if (_targetPc.getLevel() >= 82) {
        			additionalImmune = 0.25;
        		} else if (_targetPc.getLevel() >= 80) {
        			additionalImmune = 0.20;
        		} else if (_targetPc.getLevel() >= 78) {
        			additionalImmune = 0.15;
        		} else if (_targetPc.getLevel() >= 76) {
        			additionalImmune = 0.10;
        		} else if (_targetPc.getLevel() >= 74) {
        			additionalImmune = 0.05;
        		}
        		dmg -= (dmg * (Config.RATE_IMMUNE_HARM_OTHERS + additionalImmune));
                }
            }
        
        // Damage reduction of skills, cooking, etc.
        // traitors shield change damage reduction
        if (_targetPc.getInventory().checkEquipped(22263)) {
            int chance = _random.nextInt(100) + 1;
            L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22263);
            int enchant = item.getEnchantLevel();
        	if (chance <= (enchant * 2)) {
                dmg -= 50;
                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 6320));
                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 6320));
            }
        }
        if (_targetPc.getInventory().checkEquipped(222355)) { //Sacred Elven Shield
            int chance = _random.nextInt(100) + 1;
            L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(222355);
            if (chance <= item.getEnchantLevel()) {
                dmg -= 10;
                _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 14543));
                _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 14543));
            }
        }
        // Lindvior blessing
        int chance6 = _random.nextInt(100) + 1;
        if (dmg > 25) {
            if (_target != _targetNpc) {
                if (_targetPc.getInventory().checkEquipped(22204)) { // Lindvior Storm Plate Mail
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22204);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 10);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                } else if (_targetPc.getInventory().checkEquipped(22205)) { // Lindvior Storm Scale Mail
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22205);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 15);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                } else if (_targetPc.getInventory().checkEquipped(22206)) { // Lindvior Storm Leather Armor
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22206);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 20);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                } else if (_targetPc.getInventory().checkEquipped(22207)) { // Lindvior Storm Robe
                    L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22207);
                    if (chance6 <= 1 + item.getEnchantLevel()) {
                        short getMp = (short) (_targetPc.getCurrentMp() + 20);
                        _targetPc.setCurrentMp(getMp);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2188));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2188));
                    }
                }
            }
        }
        // Blessing of Fafurion
        int chance5 = _random.nextInt(100) + 1;
        if (dmg > 25) {
            if (_target != null) {
                int dmg2 = 0;
                int plus = 0;
                if (_targetPc.getInventory().checkEquipped(22200) || // Pap strength
                        _targetPc.getInventory().checkEquipped(22201) || // Pap foresight
                        _targetPc.getInventory().checkEquipped(22202) || // Pap endurance
                        _targetPc.getInventory().checkEquipped(22203)) { // Pap horsepower
                    if (chance5 <= 5) {
                        L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(22200);
                        L1ItemInstance item1 = _targetPc.getInventory().findEquippedItemId(22201);
                        L1ItemInstance item2 = _targetPc.getInventory().findEquippedItemId(22202);
                        L1ItemInstance item3 = _targetPc.getInventory().findEquippedItemId(22203);
                        if (item.getEnchantLevel() >= 7 && item.getEnchantLevel() <= 9) {
                            plus = item.getEnchantLevel() - 6;
                        } else if (item1.getEnchantLevel() >= 7 && item1.getEnchantLevel() <= 9) {
                            plus = item.getEnchantLevel() - 6;
                        } else if (item2.getEnchantLevel() >= 7 && item2.getEnchantLevel() <= 9) {
                            plus = item.getEnchantLevel() - 6;
                        } else if (item3.getEnchantLevel() >= 7 && item3.getEnchantLevel() <= 9) {
                            plus = item.getEnchantLevel() - 6;
                        } else if (item.getEnchantLevel() > 9 || item1.getEnchantLevel() > 9
                                || item2.getEnchantLevel() > 9 || item3.getEnchantLevel() > 9) {
                            plus = 3;
                        }
                        if (_targetPc.hasSkillEffect(L1SkillId.POLLUTE_WATER)) {
                            dmg2 += (40 + _random.nextInt(15) + (plus * 10)) / 2; // Half of the flute water case // the original random number 30
                        }
                        if (_targetPc.hasSkillEffect(L1SkillId.WATER_LIFE)) {
                            dmg2 += (40 + _random.nextInt(15) + (plus * 10)) * 2; // If water life is doubled // the original random number is 30
                        }
                        dmg2 += 40 + _random.nextInt(15) + (plus * 10); // Recovery rate = Basic 50 recovery + Random (1 to 30) // Original random number 30
                        _targetPc.setCurrentHp(_targetPc.getCurrentHp() + dmg2);
                        _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2187));
                        _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2187));
                    }
                }
            }
        }
        // Sacred Elven Plate Mail
        int chance66 = _random.nextInt(100) + 1;
        if (_target != null) {
            int dmg2 = 0;
            int plus = 0;
            if (_targetPc.getInventory().checkEquipped(222351)) {
                if (chance66 <= 5) {
                    if (_targetPc.hasSkillEffect(L1SkillId.POLLUTE_WATER)) {
                        dmg2 += (25 + _random.nextInt(15) + (plus * 10)) / 2;
                    }
                    if (_targetPc.hasSkillEffect(L1SkillId.WATER_LIFE)) {
                        dmg2 += (25 + _random.nextInt(15) + (plus * 10)) * 2;
                    }
                    dmg2 += 25 + _random.nextInt(15) + (plus * 10);
                    _targetPc.setCurrentHp(_targetPc.getCurrentHp() + dmg2);
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 2187));
                    _targetPc.broadcastPacket(new S_SkillSound(_targetPc.getId(), 2187));
                }
            }
        }

        // Attack players from pets and salmon
        boolean isNowWar = false;
        int castleId = L1CastleLocation.getCastleIdByArea(_targetPc);
        if (castleId > 0) {
            isNowWar = WarTimeController.getInstance().isNowWar(castleId);
        }
        if (!isNowWar) {
            if (_npc instanceof L1PetInstance) {
                dmg /= 2;
            }
            if (_npc instanceof L1SummonInstance) {
                L1SummonInstance summon = (L1SummonInstance) _npc;
                if (summon.isExsistMaster()) {
                    dmg /= 2;
                }
            }
        }

        addNpcPoisonAttack(_npc, _targetPc);

        if (_npc instanceof L1PetInstance || _npc instanceof L1SummonInstance) {
            if (_targetPc.getZoneType() == 1) {
                _isHit = false;
            }
        }

        if (dmg <= 0) {
            _isHit = false;
        }
        return (int) dmg;
    }

    // Damage calculation from NPC to NPC
    private int calcNpcNpcDamage() {
        if (_targetNpc == null || _npc == null)
            return 0;

        int lvl = _npc.getLevel();
        double dmg = 0;

        if (_npc instanceof L1PetInstance) {
            dmg = _random.nextInt(_npc.getNpcTemplate().get_level()) + _npc.getAbility().getTotalStr();
            dmg += (lvl / 8); // +1 dmg every 8 pet levels
            dmg += ((L1PetInstance) _npc).getDamageByWeapon();
        } else if (_npc instanceof L1SummonInstance) {
            dmg = _random.nextInt(lvl) + _npc.getAbility().getTotalStr() + 10;
        } else {
            dmg = _random.nextInt(lvl) + _npc.getAbility().getTotalStr();
        }

        if (isUndeadDamage()) {
        	int undeadDmg = _random.nextInt(39) + 1;
        	if (undeadDmg != 0) {
        		dmg = dmg + undeadDmg;
        	}
        }

        dmg = dmg * getLeverage() / 10;
        //System.out.println("pet damage " + dmg);

        dmg -= calcNpcDamageReduction();

        if (_npc.isWeaponBreaked()) { // NPCs are on a weapon break.
            dmg /= 2;
        }

        addNpcPoisonAttack(_npc, _targetNpc);

        if (_targetNpc.hasSkillEffect(ICE_LANCE)) {
            dmg = 0;
        }
        if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
            dmg = 0;
        }

        if (dmg <= 0) {
            _isHit = false;
        }

        return (int) dmg;
    }

    // Add effects by weapon attribute enchantment (PC-PC)
    private double fishAttrEnchantEffect() {
        int Attr = _weaponAttrLevel;
        double AttrDmg = 0;
        switch (_weaponAttrLevel) {
        case 1:
			AttrDmg += (Attr * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 2:
			AttrDmg += (Attr * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 3:
			AttrDmg += (Attr * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 4:
			AttrDmg += (Attr * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 5:
            AttrDmg += (Attr * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 6:
			AttrDmg += ((Attr - 5) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 7:
			AttrDmg += ((Attr - 5) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 8:
			AttrDmg += ((Attr - 5) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 9:
			AttrDmg += ((Attr - 5) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 10:
            AttrDmg += ((Attr - 5) * 2) - 1;
           // AttrDmg += (Attr - 6) + 1;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getWater() / 100;
            break;
        case 11:
			AttrDmg += ((Attr - 10) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 12:
			AttrDmg += ((Attr - 10) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 13:
			AttrDmg += ((Attr - 10) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 14:
			AttrDmg += ((Attr - 10) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 15:
            AttrDmg += ((Attr - 10) * 2) - 1;
            //AttrDmg += (Attr - 11) + 1;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getWind() / 100;
            break;
        case 16:
			AttrDmg += ((Attr - 15) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 17:
			AttrDmg += ((Attr - 15) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 18:
			AttrDmg += ((Attr - 15) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 19:
			AttrDmg += ((Attr - 15) * 2) - 1;
            //AttrDmg += (Attr - 1) + 2;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getFire() / 100;
            break;
        case 20:
            AttrDmg += ((Attr - 15) * 2) - 1;
            //AttrDmg += (Attr - 16) + 1;
           // AttrDmg -= AttrDmg * _targetPc.getResistance().getEarth() / 100;
            break;
        default:
            AttrDmg = 0;
            break;
        }
        return AttrDmg;
    }

    /**
     * Adding effects by weapon attribute enchantment (PC-NPC)
     **/
   /* private int monsterAttrEnchantEffect() {
        int AttrDmg = 0;
        int Attr = _weaponAttrLevel;
        int NpcWeakAttr = _targetNpc.getNpcTemplate().get_weakAttr();
        switch (NpcWeakAttr) {
        case 1: // 土地脆弱モンスター
            if (Attr >= 15 && Attr <= 20) {
                AttrDmg += 1 + (Attr - 15) * 2;
                //AttrDmg += 1 + (Attr - 15);
            }
            break;
        case 2: // 水脆弱モンスター
            if (Attr >= 6 && Attr <= 10) {
                AttrDmg += 1 + (Attr - 6) * 2;
                //AttrDmg += 1 + (Attr - 6);
            }
            break;
        case 4: // 火脆弱モンスター
            if (Attr >= 1 && Attr <= 5) {
                AttrDmg += (Attr - 1) * 2 + 1;
               // AttrDmg += 1 + (Attr - 1);
            }
            break;
        case 8: // 風脆弱モンスター
            if (Attr >= 11 && Attr <= 15) {
                AttrDmg += 1 + (Attr - 11) * 2;
                //AttrDmg += 1 + (Attr - 11);
            }
            break;
        default:
            AttrDmg = 0;
            break;
        }
        return AttrDmg;
    }*/

    // damage reduction by player AC
    private int calcPcDefense() {
        int ac = Math.max(0, 10 - _targetPc.getAC().getAc()); // default to 0 even if negative
        int acDefMax = _targetPc.getClassFeature().getAcDefenseMax(ac);
        return _random.nextInt(acDefMax + 1);
    }

    // Mitigation by reducing NPC damage
    private int calcNpcDamageReduction() {
        return _targetNpc.getNpcTemplate().get_damagereduction();
    }

    // Additional damage calculation due to weapon material and blessing
    private int calcMaterialBlessDmg() {
        int damage = 0;
        int undead = _targetNpc.getNpcTemplate().get_undead();
        if ((_weaponMaterial == 14 || _weaponMaterial == 17 || _weaponMaterial == 22) && (undead == 1 || undead == 3)) {
        	// silver/Mithril/Orichalcum
            damage += _random.nextInt(39) + 2;
        }
        
        if (_weaponBless == 0 && (undead == 1 || undead == 2 || undead == 3)) {
        	damage += _random.nextInt(10) + 1;
        }

        if (weapon != null && _weaponType != 20 && _weaponType != GAUNTLET && weapon.getHolyDmgByMagic() != 0
                && (undead == 1 || undead == 3)) {
            damage += weapon.getHolyDmgByMagic();
        }
        return damage;
    }

    // Change NPC attack power at nigh
    private boolean isUndeadDamage() {
        boolean flag = false;
        int undead = _npc.getNpcTemplate().get_undead();
        boolean isNight = L1GameTimeClock.getInstance().getGameTime().isNight();
        if (isNight && (undead == 1 || undead == 3)) {
            flag = true;
        }
        return flag;
    }

    // Added NPC poison attack
    private void addNpcPoisonAttack(L1Character attacker, L1Character target) {
        if (_npc.getNpcTemplate().get_poisonatk() != 0) { // Poison attack cage
            if (15 >= _random.nextInt(100) + 1) { // 15% chance of poison attack
                if (_npc.getNpcTemplate().get_poisonatk() == 1) { // Normal poison Damage 5 every 3 seconds
                    L1DamagePoison.doInfection(attacker, target, 3000, 5, false);
                } else if (_npc.getNpcTemplate().get_poisonatk() == 2) { // Silence poison
                	L1SilencePoison.doInfection(target);
                } else if (_npc.getNpcTemplate().get_poisonatk() == 4) { // Paralytic poison Paralysis for 16 seconds after 20 seconds
                    L1ParalysisPoison.doInfection(target, 12000, 8000);
                }
            }
        } else if (_npc.getNpcTemplate().get_paralysisatk() != 0) { // Paralysis attack
        }
    }

    // Calculation of MP absorption after manas staff and manas of steel
    public void calcStaffOfMana() {
        // Mana, steel mana, devil's wand
        if (_weaponId == 126 || _weaponId == 127 || _weaponId == 120 || _weaponId == 224 || _weaponId == 1012) {
            int som_lvl = _weaponEnchant + 3; // Set maximum MP absorption
            if (som_lvl < 0) {
                som_lvl = 0;
            }
            // Random acquisition of MP absorption amount
            _drainMana = _random.nextInt(som_lvl) + 1;
            // Limit maximum MP absorption to 9
            if (_drainMana > Config.MANA_DRAIN_LIMIT_PER_SOM_ATTACK) {
                _drainMana = Config.MANA_DRAIN_LIMIT_PER_SOM_ATTACK;
            }
        }
    }

    public void manaBaselard() { // Addition for mana absorption
        int MR = getTargetMr(); // Apply success rate based on demon
        if (MR >= 180)
            return;
        if (MR < _random.nextInt(180))
            _drainMana = 4;
    }

    private int getTargetMr() {
        int mr = 1;
        if (_calcType == PC_PC || _calcType == NPC_PC) {
            mr = _targetPc.getResistance().getEffectedMrBySkill();
        } else {
            mr = _targetNpc.getResistance().getEffectedMrBySkill();
        }
        return mr;
    }

    public void getAbsorHP(L1PcInstance pc, L1Character target) {
        int pcInt = pc.getAbility().getTotalInt();

        _drainHp = (_random.nextInt(5) + pcInt + _weaponEnchant) / 2;

        if (_drainHp > 0 && target.getCurrentHp() > 0) {
            if (_drainHp > target.getCurrentHp()) {
                _drainHp = target.getCurrentHp();
            }
            short newHp = (short) (target.getCurrentHp() - _drainHp);
            target.setCurrentHp(newHp);
            newHp = (short) (_pc.getCurrentHp() + _drainHp);
            pc.setCurrentHp(newHp);
        }
    }

    /**
     * Elephant Stone Golem-Horsepower Dagger
     **/
    public void calcDrainOfMana() {
        if (_weaponId == 602) {
            manaBaselard();
        }
    }

    /**
     * Elephant Stone Golem-Great Sword of Ruin *
     */
    public void ruinGreatSword(double dmg) { // See Greatsword Powerbook of 21 times ruin
        int r = _random.nextInt(100);
        if (r <= 80) {
            if (dmg <= 30) {
                _drainHp = 1;
            } else if (dmg > 30 && dmg <= 38) {
                _drainHp = 2;
            } else if (dmg > 38 && dmg <= 46) {
                _drainHp = 3;
            } else if (dmg > 46 && dmg <= 54) {
                _drainHp = 4;
            } else if (dmg > 54 && dmg <= 62) {
                _drainHp = 5;
            } else if (dmg > 62 && dmg <= 70) {
                _drainHp = 6;
            } else if (dmg > 70 && dmg <= 78) {
                _drainHp = 7;
            } else if (dmg > 78) {
                _drainHp = 8;
            }
        }
    }

    public void bloodSucker(double dmg, int enchant) { // 21 times See Powerbook
        int r = _random.nextInt(100);
        int e = enchant / 2;
        if (r <= 75) {
            if (dmg <= 30) {
                _drainHp = 1 + e;
            } else if (dmg > 30 && dmg <= 38) {
                _drainHp = 2 + e;
            } else if (dmg > 38 && dmg <= 46) {
                _drainHp = 3 + e;
            } else if (dmg > 46 && dmg <= 54) {
                _drainHp = 4 + e;
            } else if (dmg > 54 && dmg <= 63) {
                _drainHp = 5 + e;
            } else if (dmg > 63 && dmg <= 73) {
                _drainHp = 6 + e;
            } else if (dmg > 73 && dmg <= 83) {
                _drainHp = 7 + e;
            } else if (dmg > 83 && dmg <= 92) {
                _drainHp = 8 + e;
            } else if (dmg > 92) {
                _drainHp = 9 + e;
            }
            if (e <= 0) {
                e = 0;
            }
        }
    }

    public int extremeChainSword(L1PcInstance pc, L1Character target, int effect, int enchant) {
        int dmg = 0;
        int en = enchant;
        int intel = pc.getAbility().getTotalInt();
        int chance = _random.nextInt(100) + 1;
        if (chance <= en + 2) {
            _drainHp = _random.nextInt(intel / 2) + (intel * 2);
            pc.sendPackets(new S_SkillSound(target.getId(), effect));
            Broadcaster.broadcastPacket(pc, new S_SkillSound(target.getId(), effect));
        }
        return dmg;
        // return L1WeaponSkill.calcDamageReduction(target, dmg,
        // L1Skills.ATTR_WATER);
    }

    // Gaia rage
    public int getEbMP1(L1PcInstance pc, L1Character target, int effect, int enchant) {
        int dmg = 0;
        int en = enchant;
        int chance = _random.nextInt(100) + 1;
        if (chance <= en + 5) {
            _drainMana = 10;
            pc.sendPackets(new S_SkillSound(target.getId(), effect));
            Broadcaster.broadcastPacket(pc, new S_SkillSound(target.getId(), effect));
        }
        return dmg;
    }

    // Evil Reverse
    // Sword Chain Sword Crow Axe
    public int reverseEvil(L1PcInstance pc, L1Character target, int effect, int enchant) {
        int dmg = 0;
        int en = enchant;
        int strg = pc.getAbility().getTotalStr();
        int spellPower = pc.getAbility().getSp();
        int chance = _random.nextInt(100) + 1;
        if (chance <= en + 6) {
            _drainHp = _random.nextInt((strg) + spellPower);
            pc.sendPackets(new S_SkillSound(target.getId(), effect));
            Broadcaster.broadcastPacket(pc, new S_SkillSound(target.getId(), effect));
        }
        return dmg;
    }

    public void miss(L1PcInstance pc, L1Character target) {
        pc.sendPackets(new S_SkillSound(target.getId(), 13418));
        Broadcaster.broadcastPacket(pc, new S_SkillSound(target.getId(), 13418));
    }

    // Cane bow key link / Evil Trick
    public int evilTrick(L1PcInstance pc, L1Character target, int effect, int enchant) {
        int dmg = 0;
        int en = enchant;
        int chance = _random.nextInt(100) + 1;
        if (chance <= en + 2) {
            _drainMana = _random.nextInt(10);
            pc.sendPackets(new S_SkillSound(target.getId(), effect));
            Broadcaster.broadcastPacket(pc, new S_SkillSound(target.getId(), effect));
        }
        return dmg;
    }

    // ■■■■ Add poison attack on PC ■■■■
    public void addPcPoisonAttack(L1Character attacker, L1Character target) {
        int chance = _random.nextInt(100) + 1;
        if ((_weaponId == 13 || _weaponId == 44 // FOD, ancient dark elf sword
                || (_weaponId != 0 && _pc.hasSkillEffect(ENCHANT_VENOM))) // Enchantment Venom
                && chance <= 10) {
            L1DamagePoison.doInfection(attacker, target, 3000, 30, false);
        }
    }

    // Attack motion display
    public void action() {
        try {
            if (_calcType == PC_PC || _calcType == PC_NPC) {
                if (_isCritical) {
                    criticalPc();
                    if (!_pc.isGm()) {
                        _isCritical = false;
                    }
                } else {
                    actionPc();
                }
                // Key link effect
                if (_pc.getWeapon() != null && _pc.getWeapon().getItem().getType() == 17) {
                    if (_pc.getWeapon().getItem().getItemId() == 503) {
                        _pc.sendPackets(new S_SkillSound(_pc.getId(), 6983));
                        Broadcaster.broadcastPacket(_pc, new S_SkillSound(_pc.getId(), 6983));
                    } else {
                        _pc.sendPackets(new S_SkillSound(_pc.getId(), 7049));
                        Broadcaster.broadcastPacket(_pc, new S_SkillSound(_pc.getId(), 7049));
                    }
                }
            } else if (_calcType == NPC_PC || _calcType == NPC_NPC) {
                actionNpc();
            }
        } catch (Exception e) {
        }
    }

    // send player attack animation
    private void actionPc() {
        _pc.setHeading(_pc.targetDirection(_targetX, _targetY)); // Direction set
        if (_target instanceof L1NpcInstance) {
            if (((L1NpcInstance) _target).getNpcId() >= 400067 && ((L1NpcInstance) _target).getNpcId() <= 400080) {
                _isHit = false;
            }
        }
        if (_weaponType == 20) { // arrow
            if (_pc instanceof L1RobotInstance || _arrow != null) {
                if (!_pc.noPlayerCK)
                    _pc.getInventory().removeItem(_arrow, 1);
                if (_pc.getTempCharGfx() == 7967) {
                    _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 7972, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 7972, _targetX, _targetY, _isHit));
                } else if (_pc.getTempCharGfx() == 11402 || _pc.getTempCharGfx() == 8900) { // 75Rep Makeover
                    _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 8904, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 8904, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                } else if (_pc.getTempCharGfx() == 11406 || _pc.getTempCharGfx() == 8913) {// 80Rep Makeover
                    _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 8916, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 8916, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                } else if (_pc.getTempCharGfx() == 13631) {// 82Rep Makeover
                    _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 13656, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 13656, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                } else if (_pc.getTempCharGfx() == 13635) {// 85Rep Makeover
                    _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 13658, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 13658, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                } else {
                    _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 66, _targetX, _targetY, _isHit));
                    Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 66, _targetX, _targetY, _isHit));
                }
                if (_isHit) {
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                }
            } else if (_weaponId == 190) {
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 2349, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 2349, _targetX, _targetY, _isHit));
                if (_isHit) {
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                }
            } else if (_weaponId == 202011) { // Gaia's rage
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 13658, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 13658, _targetX, _targetY, _isHit));
                if (_isHit) {
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                }
            } else if (_weaponId == 10000) {
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 8771, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 8771, _targetX, _targetY, _isHit));
                if (_isHit) {
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
                }
            }
        } else if (_weaponType == GAUNTLET && _sting != null) { // Sting
            _pc.getInventory().removeItem(_sting, 1);
            if (_pc.getTempCharGfx() == 7967) {
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 7972, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 7972, _targetX, _targetY, _isHit));
            } else if (_pc.getTempCharGfx() == 11402 || _pc.getTempCharGfx() == 8900) {// 75レ Rep Makeover
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 8904, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 8904, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
            } else if (_pc.getTempCharGfx() == 11406 || _pc.getTempCharGfx() == 8913) {// 80 Rep Makeover
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 8916, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 8916, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
            } else if (_pc.getTempCharGfx() == 13631) {// 82 Rep Makeover
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 13656, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 13656, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
            } else if (_pc.getTempCharGfx() == 13635) {// 85 Rep Makeover
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 13658, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 13658, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
            } else {
                _pc.sendPackets(new S_UseArrowSkill(_pc, _targetId, 2989, _targetX, _targetY, _isHit));
                Broadcaster.broadcastPacket(_pc, new S_UseArrowSkill(_pc, _targetId, 2989, _targetX, _targetY, _isHit));
            }
            if (_isHit) {
                Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
            }
        } else {
            if (_isHit) {
                _pc.sendPackets(new S_AttackPacket(_pc, _targetId, ActionCodes.ACTION_Attack, _attackType));
                Broadcaster.broadcastPacket(_pc, new S_AttackPacket(_pc, _targetId, ActionCodes.ACTION_Attack, _attackType));
                Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _pc);
            } else {
                if (_targetId > 0) {
                	_pc.sendPackets(new S_AttackMissPacket(_pc, _targetId));
                    Broadcaster.broadcastPacket(_pc, new S_AttackMissPacket(_pc, _targetId));
                } else {
                    _pc.sendPackets(new S_AttackPacket(_pc, 0, ActionCodes.ACTION_Attack));
                    Broadcaster.broadcastPacket(_pc, new S_AttackPacket(_pc, 0, ActionCodes.ACTION_Attack));
                }
            }
        }
    }

    private void criticalPc() {
        _pc.setHeading(_pc.targetDirection(_targetX, _targetY)); // Direction set
        if (_weaponType == 20) {
            _pc.sendPackets(new S_AttackCritical(_pc, _targetId, _targetX, _targetY, _weaponType, _isHit));
            Broadcaster.broadcastPacket(_pc, new S_AttackCritical(_pc, _targetId, _targetX, _targetY, _weaponType, _isHit)); // I created a new one
        } else if (_weaponType == GAUNTLET && _sting != null) {

            _pc.sendPackets(new S_AttackCritical(_pc, _targetId, _targetX, _targetY, _weaponType, _isHit));
            Broadcaster.broadcastPacket(_pc, new S_AttackCritical(_pc, _targetId, _targetX, _targetY, _weaponType, _isHit));
        } else {
            if (_pc.isWarrior()) {
                _pc.sendPackets(new S_AttackCritical(_pc, _targetId, 99));
                Broadcaster.broadcastPacket(_pc, new S_AttackCritical(_pc, _targetId, 99));
            } else {
                if (_weaponType2 == 18) { // chainsword
                    _weaponType = 90;
                } else if (_weaponType2 == 17) { // kiringku
                    _weaponType = 91;
                } else if (_weaponType2 == 8 || _weaponType == 10) { // throwing knife gauntlet
                    _weaponType = 92;
                }
            }
            _pc.sendPackets(new S_AttackCritical(_pc, _targetId, _weaponType));
            Broadcaster.broadcastPacket(_pc, new S_AttackCritical(_pc, _targetId, _weaponType));
        }
    }

    // send NPC attack animation
    private void actionNpc() {
        int _npcObjectId = _npc.getId();
        int bowActId = 0;
        int actId = 0;

        _npc.setHeading(_npc.targetDirection(_targetX, _targetY)); // Direction set

        // If the distance to the target is 2 or more, a long-range attack
        boolean isLongRange = (_npc.getLocation().getTileLineDistance(new Point(_targetX, _targetY)) > 1);
        bowActId = _npc.getNpcTemplate().getBowActId();

        if (getActId() > 0) {
            actId = getActId();
        } else {
            actId = ActionCodes.ACTION_Attack;
        }

        if (isLongRange && bowActId > 0) {
            Broadcaster.broadcastPacket(_npc, new S_UseArrowSkill(_npc, _targetId, bowActId, _targetX, _targetY, _isHit));
        } else {
            if (_isHit) {
                if (getGfxId() > 0) {
                    Broadcaster.broadcastPacket(_npc, new S_UseAttackSkill(_target, _npcObjectId, getGfxId(), _targetX, _targetY, actId));
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _npc);
                } else {
                    Broadcaster.broadcastPacket(_npc, new S_AttackPacketForNpc(_target, _npcObjectId, actId));
                    Broadcaster.broadcastPacketExceptTargetSight(_target, new S_DoActionGFX(_targetId, ActionCodes.ACTION_Damage), _npc);
                }
            } else {
                if (getGfxId() > 0) {
                    Broadcaster.broadcastPacket(_npc, new S_UseAttackSkill(_target, _npcObjectId, getGfxId(), _targetX, _targetY, actId, 0));
                } else {
                    Broadcaster.broadcastPacket(_npc, new S_AttackMissPacket(_npc, _targetId, actId));
                }
            }
        }
    }

    // Calculate the trajectory of a tree where a missile (arrow, sting) was a mistake
    public void calcOrbit(int cx, int cy, int head) // Base point X Base point Y Direction you are facing now
    {
        float dis_x = Math.abs(cx - _targetX); // Distance to target in X direction
        float dis_y = Math.abs(cy - _targetY); // Distance to the target in the Y direction
        float dis = Math.max(dis_x, dis_y); // Distance to the target
        float avg_x = 0;
        float avg_y = 0;
        if (dis == 0) { // If it is in the same position as the target, go straight in the direction you are facing
            switch (head) {
            case 1:
                avg_x = 1;
                avg_y = -1;
                break;
            case 2:
                avg_x = 1;
                avg_y = 0;
                break;
            case 3:
                avg_x = 1;
                avg_y = 1;
                break;
            case 4:
                avg_x = 0;
                avg_y = 1;
                break;
            case 5:
                avg_x = -1;
                avg_y = 1;
                break;
            case 6:
                avg_x = -1;
                avg_y = 0;
                break;
            case 7:
                avg_x = -1;
                avg_y = -1;
                break;
            case 0:
                avg_x = 0;
                avg_y = -1;
                break;
            default:
                break;
            }
        } else {
            avg_x = dis_x / dis;
            avg_y = dis_y / dis;
        }

        int add_x = (int) Math.floor((avg_x * 15) + 0.59f); // Round with a little priority on top, bottom, left and right
        int add_y = (int) Math.floor((avg_y * 15) + 0.59f); // Round with a little priority on top, bottom, left and right

        if (cx > _targetX) {
            add_x *= -1;
        }
        if (cy > _targetY) {
            add_y *= -1;
        }

        _targetX = _targetX + add_x;
        _targetY = _targetY + add_y;
    }

    // Reflected in calculation results
    public void commit() {
        if (_isHit) {
            try {
                if (_calcType == PC_PC || _calcType == NPC_PC) {
                    commitPc();
                } else if (_calcType == PC_NPC || _calcType == NPC_NPC) {
                    commitNpc();
                }
            } catch (Exception e) {
            }
        }

        // Message for confirming damage value and accuracy
        if (!Config.ATTACK_MESSAGE) {
            return;
        }
        if (_target == null)
            return;
        if (Config.ATTACK_MESSAGE) {
            if ((_calcType == PC_PC || _calcType == PC_NPC) && !_pc.isGm()) {
                return;
            }
            if ((_calcType == PC_PC || _calcType == NPC_PC) && !_targetPc.isGm()) {
                return;
            }
        }
        String msg0 = "";
        String msg1 = "";
        String msg2 = "";
        String msg3 = "";
        if (_calcType == PC_PC || _calcType == PC_NPC) { //When the attack car is a PC
            msg0 = _pc.getName();
        } else if (_calcType == NPC_PC) { // If the attack car is an NPC
            msg0 = _npc.getName();
        }

        if (_calcType == NPC_PC || _calcType == PC_PC) { // If the target is a PC
            msg3 = _targetPc.getName();
            msg1 = "HP:" + _targetPc.getCurrentHp() + " / HR:" + _hitRate;
        } else if (_calcType == PC_NPC) { //If the target is an NPC
            msg3 = _targetNpc.getName();
            msg1 = "HP:" + _targetNpc.getCurrentHp() + " / HR:" + _hitRate;
        }
        msg2 = "DMG:" + _damage;

        if (_calcType == PC_PC || _calcType == PC_NPC) { // When the attack car is a PC
            _pc.sendPackets(new S_SystemMessage("\\fR[" + msg0 + "->" + msg3 + "] " + msg2 + " / " + msg1));
        }
        if (_calcType == NPC_PC || _calcType == PC_PC) { // If the target is a PC
            _targetPc.sendPackets(new S_SystemMessage("\\fY[" + msg0 + "->" + msg3 + "] " + msg2 + " / " + msg1));
        }
    }

    // Reflect the calculation result of the player
    private void commitPc() {
        if (_calcType == PC_PC) {
            if (_targetPc.hasSkillEffect(ICE_LANCE)) {
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_targetPc.hasSkillEffect(EARTH_BIND)) {
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_targetPc.hasSkillEffect(MOB_BASILL)) { // バジルアーリー期待未知0
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_targetPc.isGm() || _targetPc.isMonitor()) {
            	_drainMana = 0;
                _drainHp = 0;
            }
            if (_targetPc.hasSkillEffect(MOB_COCKATRICE_FREEZE)) { // コカアーリー期待未知0
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_drainMana > 0 && _targetPc.getCurrentMp() > 0) {
                if (_drainMana > _targetPc.getCurrentMp()) {
                    _drainMana = _targetPc.getCurrentMp();
                }
                short newMp = (short) (_targetPc.getCurrentMp() - _drainMana);
                _targetPc.setCurrentMp(newMp);
                newMp = (short) (_pc.getCurrentMp() + _drainMana);
                _pc.setCurrentMp(newMp);
            }

            /** Elephant Stone Golem **/

            if (_drainHp > 0 && _targetPc.getCurrentHp() > 0) {
                if (_drainHp > _targetPc.getCurrentHp()) {
                    _drainHp = _targetPc.getCurrentHp();
                }
                short newHp = (short) (_targetPc.getCurrentHp() - _drainHp);
                _targetPc.setCurrentHp(newHp);
                newHp = (short) (_pc.getCurrentHp() + _drainHp);
                _pc.setCurrentHp(newHp);
            }
            /**Elephant Stone Golem**/

            // damagePcWeaponDurability(); // Damage the weapon.

            _targetPc.receiveDamage(_pc, _damage);
        } else if (_calcType == NPC_PC) {
            if (_targetPc.hasSkillEffect(ICE_LANCE)) {
                _damage = 0;
            }
            if (_targetPc.hasSkillEffect(EARTH_BIND)) {
                _damage = 0;
            }
            if (_targetPc.hasSkillEffect(MOB_BASILL)) { // Basil Early Expectations Unknown 0
                _damage = 0;
            }
            if (_targetPc.hasSkillEffect(MOB_COCKATRICE_FREEZE)) { // Coca Early Expectations Unknown 0
                _damage = 0;
            }
            _targetPc.receiveDamage(_npc, _damage);
        }
    }

    // Reflect the calculation result in NPC
    private void commitNpc() {
        if (_calcType == PC_NPC) {
            if (_targetNpc.hasSkillEffect(ICE_LANCE)) {
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_targetNpc.hasSkillEffect(MOB_BASILL)) {
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_targetNpc.hasSkillEffect(MOB_COCKATRICE_FREEZE)) {
                _damage = 0;
                _drainMana = 0;
                _drainHp = 0;
            }
            if (_drainMana > 0) {
                int drainValue = _targetNpc.drainMana(_drainMana);
                int newMp = _pc.getCurrentMp() + drainValue;
                _pc.setCurrentMp(newMp);

                if (drainValue > 0) {
                    int newMp2 = _targetNpc.getCurrentMp() - drainValue;
                    _targetNpc.setCurrentMp(newMp2);
                }
            }

            /** Elephant Stone Golem **/

            if (_drainHp > 0) {
                int newHp = _pc.getCurrentHp() + _drainHp;
                _pc.setCurrentHp(newHp);
            }
            
            /** Elephant Stone Golem **/
            damageNpcWeaponDurability(); // Damage the weapon.

            _targetNpc.receiveDamage(_pc, _damage);
        } else if (_calcType == NPC_NPC) {
            if (_targetNpc.hasSkillEffect(ICE_LANCE)) {
                _damage = 0;
            }
            if (_targetNpc.hasSkillEffect(EARTH_BIND)) {
                _damage = 0;
            }
            if (_targetNpc.hasSkillEffect(MOB_BASILL)) {
                _damage = 0;
            }
            if (_targetNpc.hasSkillEffect(MOB_COCKATRICE_FREEZE)) {
                _damage = 0;
            }
            _targetNpc.receiveDamage(_npc, _damage);
        }
    }

    // Counter barrier
    // send Attack animation
    public void actionCounterBarrier() {
        if (_calcType == PC_PC) {
            if (_pc == null)
                return;
            _pc.setHeading(_pc.targetDirection(_targetX, _targetY)); // Direction set
            _pc.sendPackets(new S_AttackMissPacket(_pc, _targetId));
            _pc.broadcastPacket(new S_AttackMissPacket(_pc, _targetId), _target);
            _pc.sendPackets(new S_DoActionGFX(_pc.getId(), ActionCodes.ACTION_Damage));
            _pc.broadcastPacket(new S_DoActionGFX(_pc.getId(), ActionCodes.ACTION_Damage));
            _pc.sendPackets(new S_SkillSound(_targetId, 10710));
            _pc.broadcastPacket(new S_SkillSound(_targetId, 10710));
        } else if (_calcType == NPC_PC) {
            if (_npc == null || _target == null)
                return;
            int actId = 0;
            _npc.setHeading(_npc.targetDirection(_targetX, _targetY)); // Direction set
            if (getActId() > 0) {
                actId = getActId();
            } else {
                actId = ActionCodes.ACTION_Attack;
            }
            if (getGfxId() > 0) {
                _npc.broadcastPacket(new S_UseAttackSkill(_target, _npc.getId(), getGfxId(), _targetX, _targetY, actId, 0), _target);
            } else {
                _npc.broadcastPacket(new S_AttackMissPacket(_npc, _targetId, actId), _target);
            }
            _npc.broadcastPacket(new S_DoActionGFX(_npc.getId(), ActionCodes.ACTION_Damage));
            _npc.broadcastPacket(new S_SkillSound(_targetId, 10710));
        }
    }

    // send Attack animation for mortal body
    public void actionMortalBody() {
        if (_calcType == PC_PC) {
            if (_pc == null || _target == null)
                return;
            _pc.setHeading(_pc.targetDirection(_targetX, _targetY)); // Direction set
            S_UseAttackSkill packet = new S_UseAttackSkill(_pc, _target.getId(), 6519, _targetX, _targetY, ActionCodes.ACTION_Attack, false);
            _pc.sendPackets(packet);
            _pc.broadcastPacket(packet, _target);
            _pc.sendPackets(new S_DoActionGFX(_pc.getId(), ActionCodes.ACTION_Damage));
            _pc.broadcastPacket(new S_DoActionGFX(_pc.getId(), ActionCodes.ACTION_Damage));
        } else if (_calcType == NPC_PC) {
            if (_npc == null || _target == null)
                return;
            _npc.setHeading(_npc.targetDirection(_targetX, _targetY)); // Direction set
//            _npc.broadcastPacket(new S_SkillSound(_target.getId(), 6519));
            _npc.broadcastPacket(new S_DoActionGFX(_npc.getId(), ActionCodes.ACTION_Damage));
        }
    }

    // Determine if the counter barrier is effective against the opponent's attack
    public boolean isShortDistance() {
        boolean isShortDistance = true;
        if (_calcType == PC_PC) {
            if (_weaponType == 20 || _weaponType == GAUNTLET || _weaponType2 == 17 || _weaponType2 == 19
                    || _pc.hasSkillEffect(L1SkillId.ARMOUR_BREAK)) {
                isShortDistance = false;
            }
        } else if (_calcType == NPC_PC) {
            if (_npc == null)
                return false;
            boolean isLongRange = (_npc.getLocation().getTileLineDistance(new Point(_targetX, _targetY)) > 1);
            int bowActId = _npc.getNpcTemplate().getBowActId();
            // If the distance is 2 or more and there is an action ID of the attacker's bow, one attack
            if (isLongRange && bowActId > 0) {
                isShortDistance = false;
            }
        }
        return isShortDistance;
    }

    // Reflects the damage of the counter barrier
    public void commitCounterBarrier() {
        int damage = calcCounterBarrierDamage();
        if (damage == 0) {
            return;
        }
        if (_calcType == PC_PC) {
            _pc.receiveCounterBarrierDamage(_targetPc, damage);
        } else if (_calcType == NPC_PC) {
            _npc.receiveCounterBarrierDamage(_targetPc, damage);
        }
    }

    // reflect DK mortal body damage
    public void commitMortalBody() {
        int ac = Math.max(0, 10 - _targetPc.getAC().getAc());
        int damage = ac / 2;

        if (damage == 0) {
            return;
        }
        if (damage <= 40) {
            damage = 40;
        }
        if (_calcType == PC_PC) {
            _pc.receiveDamage(_targetPc, damage);
        } else if (_calcType == NPC_PC) {
            _npc.receiveDamage(_targetPc, damage);
        }
    }

    // Calculate the damage of Counter Barrier
    private int calcCounterBarrierDamage() {
        double damage = 0;
        L1ItemInstance weapon = null;
        weapon = _targetPc.getWeapon();
        if (weapon != null) {
            if (weapon.getItem().getType() == 3) {
                damage = Math.round((weapon.getItem().getDmgLarge() + weapon.getEnchantLevel() + weapon.getItem().getDmgModifier()) * 2);
            }
        }
        return (int) damage;
    }

    // Calculate Warrior Titan Damage
    private int calcTitanDamage() {
        double damage = 0;
        L1ItemInstance weapon = null;
        if (_targetPc.hasSkillEffect(L1SkillId.SLAYER) && _targetPc.getSecondWeapon() != null && _targetPc.getSlayerSwich() == 1) {
            weapon = _targetPc.getSecondWeapon();
        } else {
            weapon = _targetPc.getWeaponSwap();
        }
        if (weapon != null) {
            damage = Math.round((weapon.getItem().getDmgLarge() + weapon.getEnchantLevel() + weapon.getItem().getDmgModifier()) * 1.5);
        }
        //System.out.println("damage " + damage);
        return (int) damage;
    }

    // Damage the weapon. In the case of a hard skin NPC, the damage probability is 5% blessed weapons are 2%
    private void damageNpcWeaponDurability() {
        int chance = 5; // General weapon
        int bchance = 2; // Blessed weapon damage probability

        /*
         * Do nothing if you are in an undamaged NPC, bare hands, undamaged weapons, or during SOF.
         */
        if (_calcType != PC_NPC || !_targetNpc.getNpcTemplate().is_hard() || _weaponType == 0
                || weapon.getItem().get_canbedmg() == 1 || _pc.hasSkillEffect(SOUL_OF_FLAME)) {
            return;
        }
        // Normal weapons / cursed weapons
        if ((_weaponBless == 1 || _weaponBless == 2) && ((_random.nextInt(100) + 1) < chance)) {
            // \f1 Your %0 has been damaged.
            _pc.sendPackets(new S_ServerMessage(268, weapon.getLogName()));
            _pc.getInventory().receiveDamage(weapon);
            if (_pc.getInventory().checkItem(40317, 1)) {
                _pc.getInventory().consumeItem(40317, 1);
                _pc.getInventory().recoveryDamage(weapon);
            }
        }
        // Blessed weapon
        if (_weaponBless == 0 && ((_random.nextInt(100) + 1) < bchance)) {
            // \f1Your %0 has been corrupted.
            _pc.sendPackets(new S_ServerMessage(268, weapon.getLogName()));
            _pc.getInventory().receiveDamage(weapon);
            if (_pc.getInventory().checkItem(40317, 1)) {
                _pc.getInventory().consumeItem(40317, 1);
                _pc.getInventory().recoveryDamage(weapon);
            }
        }
    }

    // Attribute arrow
    private int attrArrow(L1ItemInstance arrow, L1NpcInstance npc) {
        int itemId = arrow.getItem().getItemId();
        int damage = 0;
        int NpcWeakAttr = _targetNpc.getNpcTemplate().get_weakAttr();
        if (itemId == 820014) { // Water Spirit Battle Arrow
            if (NpcWeakAttr == 4) {
                damage = 3;
            }
        } else if (itemId == 820015) { // Wind Spirit Battle Arrow
            if (NpcWeakAttr == 8) {
                damage = 3;
            }
        } else if (itemId == 820016) { // Earth Spirit Battle Arrow
            if (NpcWeakAttr == 1) {
                damage = 3;
            }
        } else if (itemId == 820017) { // Fire Spirit Battle Arrow
            if (NpcWeakAttr == 2) {
                damage = 3;
            }
        }
        return damage;
    }

    /**
     * Status + weapon accuracy correction
     **/
    private int pcAddHit() {
        int value = 0;
        
        if (_weaponType == 17) { // kiringku
        	value = 99;
        }

        if (_weaponType == BOW || _weaponType == GAUNTLET) { // For bows, see DEX value
            value += CalcStat.calcDEXBonusHit(_pc.getAbility().getBaseDex());
        } else {
            value += CalcStat.calcSTRBonusHit(_pc.getAbility().getBaseStr());
        }

        if (_weaponType != BOW && _weaponType != GAUNTLET) {
            value += _weaponAddHit + _pc.getHitup() + (_weaponEnchant / 2);
        } else {
            value += _weaponAddHit + _pc.getBowHitup() + (_weaponEnchant / 2);
        }
        return value;
    }

    // Target PC avoidance skill calculation
    private int toPcSkillHit() {
        int value = 0;
//        if (_targetPc.hasSkillEffect(UNCANNY_DODGE)) {
//            value -= 6;
//        }
        if (_targetPc.hasSkillEffect(FEAR)) {
            value += 5;
        }
//        if (_targetPc.hasSkillEffect(MIRROR_IMAGE)) {
//            value -= 6;
//        }
        return value;
    }

    // final hit operation
    private boolean hitRateCal(int attackDice, int defenderDice, int fumble, int critical) {
        if (attackDice <= fumble) {
            _hitRate = 0;
            return true;
        } else if (attackDice >= critical) {
            _hitRate = 95;
        } else {
            if (attackDice > defenderDice) {
                _hitRate = 95;
            } else if (attackDice <= defenderDice) {
                _hitRate = 0;
                return true;
            }
        }
        return false;
    }

    /**
     * Target PC DD operation
     **/
    private int toPcDD(int dv) {
        if (_targetPc.getAC().getAc() >= 0) {
            return 10 - _targetPc.getAC().getAc();
        } else {
            return 10 + _random.nextInt(dv) + 1;
        }
    }

    // rumtis Earrings Probability add Damage
    private double rumtisAddDamage() {
        int dmg = 0;
        if (_calcType == PC_PC || _calcType == PC_NPC) {
            L1ItemInstance item = _pc.getInventory().checkEquippedItem(222340);
            if (item != null && item.getEnchantLevel() >= 5) {
                if (_random.nextInt(100) < 2 + item.getEnchantLevel() - 5) {
                    dmg = 20;
                    _pc.sendPackets(new S_SkillSound(_pc.getId(), 13931));
                    _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 13931));
                }
            }

            L1ItemInstance item2 = _pc.getInventory().checkEquippedItem(222341);
            if (item2 != null && item2.getEnchantLevel() >= 4) {
                if (_random.nextInt(100) < 2 + item2.getEnchantLevel() - 4) {
                    dmg = 20;
                    _pc.sendPackets(new S_SkillSound(_pc.getId(), 13931));
                    _pc.broadcastPacket(new S_SkillSound(_pc.getId(), 13931));
                }
            }
        }
        return dmg;
    }

    // rumtis Earrings Probability damage reduction
    private int rumtisBlockDamage() {
        int damage = 0;
        if (_calcType == NPC_PC || _calcType == PC_PC) {
            L1ItemInstance item = _targetPc.getInventory().checkEquippedItem(22229);
            if (item != null && item.getEnchantLevel() >= 5) {
                if (_random.nextInt(100) < 2 + item.getEnchantLevel() - 5) {
                    damage = 20;
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12118), true);
                }
            }

            L1ItemInstance item2 = _targetPc.getInventory().checkEquippedItem(222337);
            if (item2 != null && item2.getEnchantLevel() >= 4) {
                if (_random.nextInt(100) < 2 + item2.getEnchantLevel() - 4) {
                    damage = 20;
                    _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 12118), true);
                }
            }
        }
        return damage;
    }
    
	public static double calcMrDefense(int MagicResistance, double dmg) {
		double cc = 0;
		if (MagicResistance <= 19) {
			cc = 0.05;
		} else if (MagicResistance <= 29) {
			cc = 0.07;
		} else if (MagicResistance <= 39) {
			cc = 0.1;
		} else if (MagicResistance <= 49) { 
			cc = 0.12;
		} else if (MagicResistance <= 59) {
			cc = 0.17;
		} else if (MagicResistance <= 69) {
			cc = 0.20;
		} else if (MagicResistance <= 79) {
			cc = 0.22;
		} else if (MagicResistance <= 89) {
			cc = 0.25;
		} else if (MagicResistance <= 99) {
			cc = 0.27;
		} else if (MagicResistance <= 110) {
			cc = 0.31;
		} else if (MagicResistance <= 120) {
			cc = 0.32;
		} else if (MagicResistance <= 130) {
			cc = 0.34;
		} else if (MagicResistance <= 140) {
			cc = 0.36;
		} else if (MagicResistance <= 150) {
			cc = 0.38;
		} else if (MagicResistance <= 160) {
			cc = 0.40;
		} else if (MagicResistance <= 170) {
			cc = 0.42;
		} else if (MagicResistance <= 180) {
			cc = 0.44;
		} else if (MagicResistance <= 190) {
			cc = 0.46;
		} else if (MagicResistance <= 200) {
			cc = 0.48;
		} else if (MagicResistance <= 220) {
			cc = 0.49;
		} else {
			cc = 0.51;
		}

		dmg -= dmg * cc;

		if (dmg < 0) {
			dmg = 0;
		}

		return dmg;
	}

    public void ArmorDestory() {
        for (L1ItemInstance armorItem : _targetPc.getInventory().getItems()) {
            if (armorItem.getItem().getType2() == 2 && armorItem.getItem().getType() == 2) {
                int armorId = armorItem.getItemId();
                L1ItemInstance item = _targetPc.getInventory().findEquippedItemId(armorId);
                if (item != null) {
                    int chance = _random.nextInt(100) + 1;
                    if (item.get_durability() == (armorItem.getItem().get_ac() * -1)) {
                        break;
                    } else {
                        if (chance <= 15) {
                            item.set_durability(item.get_durability() + 1);
                            _targetPc.getInventory().updateItem(item, L1PcInventory.COL_DURABILITY);
                            _targetPc.sendPackets(new S_SkillSound(_targetPc.getId(), 14549));
                            _targetPc.getAC().addAc(1);
                            _targetPc.sendPackets(new S_OwnCharAttrDef(_targetPc));
                            _targetPc.sendPackets(new S_ServerMessage(268, armorItem.getLogName()));
                            Broadcaster.broadcastPacket(_targetPc, new S_SkillSound(_targetPc.getId(), 14549));
                        }
                    }
                }
            }
        }
    }
}