package com.renamer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.commons.Remapper;
import jdk.internal.org.objectweb.asm.commons.RemappingClassAdapter;
import jdk.internal.org.objectweb.asm.tree.*;

import com.renamer.provider.ClassProvider;
import com.renamer.provider.ClassProviderImpl;
import com.renamer.tree.ClassMember;
import com.renamer.tree.BranchedClassNode;
import com.renamer.tree.MemberNode;
import com.renamer.util.JarLoader;


public class Renamer {

	public static void main(String... args) throws IOException {
		new Renamer("C:\\Users\\Inspiron\\Documents\\inubot\\gamepack_deob.jar",
				"C:\\Users\\Inspiron\\Documents\\inubot\\cache\\gamepack_deob.jar");
	}

	public Renamer(String in, String out) throws IOException {
		ClassProvider provider = new ClassProviderImpl();
		JarLoader loader = new JarLoader(new File(in));
		loader.load();
		Map<String, ClassMember> classes =
				loader.getInterfaceNodes();
		Renamer renamer = new Renamer(classes);
		Map<String, BranchedClassNode> tree = renamer.buildTree(provider);
		Remapper remapper = renamer.rename(tree);
		Renamer.process(remapper, loader.getClassNodes());
		loader.writeClasses(remapper);
		loader.dump(new File(out));
		Renamer.dumpMappings(tree, new File(out.replace(".jar", ".txt")));
	}

	public static void process(Remapper remapper, Map<String, ClassNode> classes) {
		for (Map.Entry<String, ClassNode> entry : classes.entrySet()) {
			ClassNode node = new ClassNode();
			RemappingClassAdapter adapter = new RemappingClassAdapter(node,
					remapper);
			entry.getValue().accept(adapter);
			processReflection(remapper, node);
			classes.put(entry.getKey(), node);
		}
	}

	public static void dumpMappings(Map<String, BranchedClassNode> tree,
			File out) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(out));
		for (BranchedClassNode node : tree.values()) {
			if (!node.external) {
				String newName = node.getNewName();
				if (newName != null) {
					writer.newLine();
					writer.write("	Old Class:" + node.name + " New Class:"
							+ node.getNewName());
					writer.newLine();
					writer.write("Fields Renamed \n");
					writer.newLine();
					for (MemberNode field : node.getFields()) {
						String desc = field.desc;
						String name_old = field.name;
						String name_new = node.getFieldName(name_old, desc);
						if (!name_old.equals(name_new)) {
							writer.write("		Old Field: "+name_old+" New: "+name_new+" Field Signature: "+desc);
							writer.newLine();
						}
					}
					writer.newLine();
					writer.write("Methods Renamed \n");
					writer.newLine();
					for (MemberNode method : node.getMethods()) {
						String desc = method.desc;
						String name_old = method.name;
						String name_new = node.getMethodName(name_old,
								method.desc);
						if (!name_old.equals(name_new)) {
							writer.write("		Old Method Name: "+name_old+" New Name: "+name_new+" Method Signature: "+desc);
							writer.newLine();
						}
					}
				} else {
					writer.newLine();
					writer.write("	Class:"+node.name);
					writer.newLine();
					writer.write("Fields Renamed \n");
					writer.newLine();
					for (MemberNode field : node.getFields()) {
						String desc = field.desc;
						String name_old = field.name;
						String name_new = node.getFieldName(name_old, desc);
						if (!name_old.equals(name_new)) {
							writer.write("		Old Field: "+name_old+" New: "+name_new+" Field Signature: "+desc);
							writer.newLine();
						}
					}
					writer.newLine();
					writer.write("Methods Renamed \n");
					writer.newLine();
					for (MemberNode method : node.getMethods()) {
						String desc = method.desc;
						String name_old = method.name;
						String name_new = node.getMethodName(name_old,
								method.desc);
						if (!name_old.equals(name_new)) {
							writer.write("		Old Method Name: "+name_old+" New Name: "+name_new+" Method Signature: "+desc);
							writer.newLine();
						}
					}
				}
			}
		}
		writer.flush();
	}

	@SuppressWarnings("unchecked")
	private static void processReflection(Remapper remapper, ClassNode node) {
		for (MethodNode mn : (List<MethodNode>) node.methods) {
			InsnList insns = mn.instructions;
			ListIterator<AbstractInsnNode> iterator = insns.iterator();
			while (iterator.hasNext()) {
				AbstractInsnNode insn = iterator.next();
				if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
					MethodInsnNode min = (MethodInsnNode) insn;
					if (min.desc
							.equals("(Ljava/lang/String;)Ljava/lang/Class;")) {
						AbstractInsnNode push = insn.getPrevious();
						if (push.getOpcode() == Opcodes.LDC) {
							LdcInsnNode lin = (LdcInsnNode) push;
							lin.cst = remapper.map(
									((String) lin.cst).replace('.', '/'))
									.replace('/', '.');
						}
					}
				}
			}
		}
	}

	private Map<String, ClassMember> classes;

	private int fieldPostfix = 1, methodPostfix = 1, classPostfix = 1;

	public Renamer(Map<String, ClassMember> classes) {
		this.classes = classes;
	}

	// TODO: Pass renaming rules to this method.
	public Remapper rename(Map<String, BranchedClassNode> tree) {
		for (BranchedClassNode node : tree.values()) {
			boolean cont;
			do {
				cont = false;
				if (node.rename("Nigger" + classPostfix) && !node.external) {
					++classPostfix;
					cont = true;
				}
			} while (cont);
		}
		SimpleRemapper remapper = new SimpleRemapper(tree);
		for (BranchedClassNode node : tree.values()) {
			if (node.name.startsWith("jag") && node.name.startsWith("jac")) {
				continue;
			}
			if (!node.external) {
				for (MemberNode field : node.getFields()) {
					if (node.renameField(field, getFieldName(remapper,
							field.desc, fieldPostfix))) {
						++fieldPostfix;
					}
				}
				for (MemberNode method : node.getMethods()) {
					if(method.name.length() < 3) {
						if (node.renameMethod(method, "method" + methodPostfix)) {
							++methodPostfix;
						}
					}
				}
			}
		}
		return remapper;
	}

	private String getFieldName(Remapper remapper, String desc, int idx) {
		char[] chars = desc.toCharArray();
		int i = 0;
		for (int len = chars.length; i < len; ++i) {
			if (chars[i] != '[') {
				break;
			}
		}
		StringBuilder builder = new StringBuilder();
		switch (chars[i]) {
		case 'I':
			builder.append("anInt");
			break;
		case 'Z':
			builder.append("aBoolean");
			break;
		case 'B':
			builder.append("aByte");
			break;
		case 'S':
			builder.append("aShort");
			break;
		case 'C':
			builder.append("aChar");
			break;
		case 'J':
			builder.append("aLong");
			break;
		case 'F':
			builder.append("aFloat");
			break;
		case 'D':
			builder.append("aDouble");
			break;
		case 'L':
			String name = remapper
					.map(desc.substring(i + 1, desc.indexOf(";")));
			name = name.substring(name.lastIndexOf('/') + 1);
			char first = name.toLowerCase().charAt(0);
			if (first == 'a' || first == 'e' || first == 'i' || first == 'o'
					|| first == 'u') {
				builder.append("an");
			} else {
				builder.append("a");
			}
			builder.append(name);
			while (i-- > 0) {
				builder.append("Array");
			}
			char last = builder.charAt(builder.length() - 1);
			if (last >= '0' && last <= '9') {
				builder.append("_");
			}
			return builder.append(idx).toString();
		default:
			builder.append("aField");
		}
		while (i-- > 0) {
			builder.append("Array");
		}
		return builder.append(idx).toString();
	}

	public Map<String, BranchedClassNode> buildTree(ClassProvider provider) {
		boolean complete;
		do {
			complete = true;
			classes: for (ClassMember node : classes.values()) {
				String name = node.name;
				for (String parent : node.parents) {
					ClassMember p = classes.get(parent);
					if (p == null) {
						p = provider.getClass(parent);
						if (p != null) {
							classes.put(parent, p);
							complete = false;
							break classes;
						} else {
							/*System.out.println("Class " + parent
									+ " could not be provided for class "
									+ node.name + ".");*/
						}
					}
					if(p != null) {
					List<String> children = p.children;
					if (!children.contains(name)) {
						children.add(name);
					}
					}
				}
			}
		} while (!complete);
		Map<String, BranchedClassNode> tree = new HashMap<String, BranchedClassNode>();
		for (ClassMember cn : classes.values()) {
			tree.put(cn.name, new BranchedClassNode(cn.name, cn.access,
					cn.superName, cn.external, cn.fields, cn.methods));
		}
		for (BranchedClassNode in : tree.values()) {
			ClassMember cn = classes.get(in.name);
			if (cn != null) {
				for (String parent : cn.parents) {
					BranchedClassNode p = tree.get(parent);
					if (p == null) {
						System.err.println("Parent " + parent + " not found!");
					} else {
						in.addParent(p);
					}
				}
				for (String child : cn.children) {
					in.addChild(tree.get(child));
				}
			}
		}
		return tree;
	}
}
